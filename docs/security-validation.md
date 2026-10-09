# FinTry güvenlik geliştirmesi — teslim ve doğrulama raporu

Tarih: 9 Ekim 2026. Temel develop: 608ce7424fd10db428cf470469eeee6fa6bdc467.
Çalışma dalı: feature/security-jwt. Develop'a merge yapılmadı.

## Tamamlanan kapsam

- Spring Security stateless Bearer doğrulaması; RS256 JWT, runtime RSA anahtarları.
- BCrypt cost=12 parola hashleme, güvenli kayıt/giriş, USER/ADMIN rol uygulaması.
- Kayıtta kullanıcı ve sıfır bakiyeli hesabın atomik oluşturulması.
- Hesap/portföy/geçmiş/işlemlerde servis seviyesinde sahiplik kontrolü; ADMIN
  başka kullanıcıların özel verilerine otomatik erişim kazanmaz.
- Fiyat/enstrüman yönetimi, bakiye talebi onayı ve kullanıcı/talep listeleri ADMIN.
- Eski parolasız kayıt ve keyfi başlangıç bakiyesi HTTP yolları kapalı.
- Frontend giriş/kayıt, bellek oturumu, expiry/401 temizliği, korumalı rotalar,
  /me entegrasyonu ve yönetim ekranı; sabit userId kaldırıldı.
- V3 migration, eski kullanıcı/ilk ADMIN provisioning prosedürü ve offline hash aracı.
- Otomatik backend/frontend testleri ve GitHub Actions regresyon workflow'u.

## Doğrulama

Cloud'da Java 21 tam JDK, Node 24.19 ve Docker 28.4 kullanıldı.
DB yalnız Testcontainers postgres:16 / PostgreSQL 16.15, dinamik portlu
fintry_isolated_test oldu. Testcontainers reuse=false; gerçek DB fallback yok.
JWT test anahtarları her JVM çalıştırmasında rastgele oluşturuldu; kaynakta yok.

| Kontrol | Sonuç |
|---|---|
| Maven clean verify | BUILD SUCCESS; 74 test, 0 failure, 0 error, 0 skipped |
| FinancialCoreTests | 17 başarılı |
| FinancialHttpTests | 7 başarılı; HTTP istekleri gerçek imzalı token kullanır |
| FinancialPolicyTests | 4 başarılı |
| FinTryApplicationTests | 1 başarılı; izole DB/port/sürüm kontrolü |
| MigrationTests | 16 başarılı |
| AuthenticationMigrationTests | 3 başarılı |
| SecurityTests | 25 başarılı |
| LoginRateLimiterTests | 1 başarılı |
| Frontend Vitest | 12 başarılı, 2 dosya |
| Frontend ESLint | Başarılı |
| Frontend production build | Başarılı |
| git diff --check | Başarılı |
| Offline provisioning launcher | Console olmadan güvenli ret; Spring/DB başlatılmadı |
| Paketleme | V3 içeren executable jar üretildi |

Son backend komutu (Cloud ortamına özel JDK/proxy/CA ve Maven cache seçimiyle):
`sh mvnw -B -s work/maven-settings.xml -Dmaven.repo.local=/workspace/.cache/fintry-m2 clean verify`.
Standart ortamda Java 21 JDK ve Docker ile `sh mvnw -B clean verify` yeterlidir.
Frontend: `npm test`, `npm run lint`, `npm run build`.

İlk denemeler gizlenmedi: Maven proxy/CA ve eksik tam JDK nedeniyle test öncesi
hatalar oluştu. İlk tam çalışmada CORS bean belirsizliği context'i engelledi;
explicit qualifier ile düzeltildi. Sonraki çalışmada finansal HTTP oturum
fixture'ları ve migration fixture ID'si hata verdi; gerçek token ve explicit ID
ile düzeltildi. Düzeltilmiş tam test çalışması 72/72 geçti; ek rol iddiası ve
limiter entegrasyon testleriyle son clean verify 74/74 geçti. Test atlama veya
security filter kapatma yapılmadı.

Regresyonlarda fraction/8–16 ondalık değerler, eşzamanlı alım/satım, fazla
harcama/satış engeli, tek bakiye onayı, kilit timeout ve transaction rollback
korundu. Güvenlik testleri gerçek token imzası/başka anahtar/algoritma/issuer/
audience/exp/nbf/subject, DB rol düşürme/devre dışı bırakma, HTTP ve doğrudan
service erişimi, ID/rol injection, sıfır hesap ve kayıt rollback/yarışı doğrular.

## Korunan dosyalar ve migration

V1__legacy_schema.sql, V2__financial_integrity.sql, FinancialPolicy,
FinancialLocks ve FinancialSchemaVerifier develop ile birebir aynı kaldı.
V1→V2→V3 boş kurulum ve V2→V3 veri koruma/çakışma testleri yalnız geçici
container'da çalıştı. Eski USER/ADMIN ID ve rolleri ile finansal satırlar korunur.
Eski kullanıcıların password_hash alanı null, enabled=false olur.

Gerçek fintry_db'ye erişilmedi veya migration uygulanmadı. Yerel eski şema için
önce ayrıca onaylı V1 baseline→V2; ardından ayrıca onaylı V3 gerekir. Yeni
uygulama eski şemada başlatılmamalı; Flyway varsayılan kapalı, ddl-auto=validate,
baseline-on-migrate=false, clean-disabled=true kalır.

API sözleşmesi: docs/security-api.md. Geçiş/runtime/provisioning:
docs/security-migration.md. Kayıt/giriş dışında tüm API oturum ister;
POST /users ve POST /accounts/user/{userId} kapalıdır. Trade gövdesi
{instrumentId,quantity}, talep gövdesi {requestedAmount}; userId artık kabul edilmez.

## Kalan sınırlar ve yerelde gerekenler

- Refresh token ve server-side logout yok; sayfa yenileme yeniden giriş ister.
  Çıkış sonrası ele geçirilmiş access token süresi dolana kadar kullanılabilir;
  hesabı kapatma sonraki istekte erişimi durdurur.
- Eski kullanıcı parola belirleme ve ilk ADMIN kontrollü yerel provisioning ister;
  public reset/rol yükseltme API'si veya ortak başlangıç parolası yoktur.
- Production RSA anahtarları ve DB secrets ayrıca sağlanmalı. Eski DB parolası
  kaynak dosyadan kaldırıldı; git geçmişinde kalabilir, yerelde rotate edilmelidir.
- HTTPS/CSP, reverse proxy ve çok node için ortak auth limiter hosting ayarıdır.
  Mevcut limiter tek instance belleğinde ve gerçek socket IP'sine göredir.
- Anahtar rotasyonu tek key pair'i değiştirerek eski token'ları iptal eder;
  kesintisiz çok anahtarlı JWKS rotasyonu ayrı kapsamdır.
- Finansal JSON sayılarının JavaScript hassasiyet sınırı sürer; girişte decimal
  metin kullanımı yeni yuvarlama kaybını önler, yanıt sözleşmesi değişmedi.
- Finansal idempotency eklenmedi; frontend otomatik write retry yapmaz.
- GitHub Actions workflow'u eklendi; Cloud test sonuçları hosted CI sonucu değildir.

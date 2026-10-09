# Güvenli auth geçişi ve çalıştırma gereksinimleri

Bu doküman gerçek veritabanında işlem yapma onayı değildir. Cloud geliştirmesi
sırasında gerçek fintry_db'ye bağlanılmaz. Yerel kullanıcı migration'ı daha sonra
ayrı çalışma ve onay kapsamında gerçekleştirir.

## V1 → V2 → V3

V1/V2 dosyaları değiştirilmez. V3 yalnız users tablosuna nullable password_hash,
varsayılan false enabled, etkin hesap için hash zorunluluğu ve normalleştirilmiş
e-posta benzersizlik indeksini ekler. Kullanıcı ID/username/email/rol değerleri,
hesaplar, portföy, geçmiş, talep durumları ve finansal kısıtlar değişmez.
Büyük/küçük harf veya dış boşluk nedeniyle çakışan mevcut e-postalar varsa V3
transaction'ı durur; otomatik kimlik birleştirme, silme veya parola atama yapılmaz.

Yerel eski Hibernate şeması için önce financial-core-migration.md rehberiyle
şema/hedef/yedek/restore doğrulanmalı ve açık V1 baseline → V2 uygulanmalıdır.
V3 ancak V2 hazır ve Flyway history doğrulanmışken uygulanır. Boş kurulumda
baseline yapılmaz; V1 → V2 → V3 çalıştırılır. V3'ü tek başına eski şemaya
uygulamayın, V2'yi tamamlanmış saymayın. Flyway clean/repair veya ddl-auto=update
ile hataları örtmeyin. V3 başarısızlığı V2'yi geri almaz.

1. Hedef sunucu, port, database ve schema'yı yerelde doğrulayın; yedek/restore
   testi yapın. Eski finansal yazıcıları durdurun.
2. V1/V2 geçişini mevcut rehbere göre ayrı bakım penceresinde tamamlayın.
3. V2 kopyasında `lower(btrim(email))` çakışmalarını, kullanıcı rollerini ve
   kaynak kimliklerini inceleyin; çakışmalarda kullanıcı sahibini doğrulayarak
   ayrı veri düzeltme kararı alın. Bu Cloud çalışması veri düzeltmez.
4. Yeni migration dosyasını reviewed release'ten alın. Aynı Flyway sürümü ve
   PostgreSQL modülüyle açık hedef üzerinde V3'ü ayrı onaydan sonra çalıştırın.
5. Flyway validate ve öncesi/sonrası ID/rol/finansal kayıt karşılaştırması yapın.
6. Parolası olmayan tüm eski kullanıcılar kapalı kalır. Aşağıdaki provisioning
   işlemi doğrulanmış kullanıcı için tamamlanmadan giriş açılmaz.
7. Yeni uygulamayı Flyway kapalı, ddl-auto=validate ile başlatın.
   SecurityConfig için RSA anahtarları ayrıca sağlanmalıdır. Eski şemada
   uygulamanın başlamaması beklenen davranıştır.

## Eski kullanıcılar

- Her hesap sahibini güvenilir ayrı kanaldan doğrulayın. Sadece e-posta/ID
  söylemek kimlik kanıtı değildir. Yeni kullanıcı kaydı eski ID'yi devralmaz.
- Kullanıcıya ait yeni güçlü parolayı ortak varsayılan kullanmadan belirleyin.
  Parola komut satırı argümanına, kaynak koda, SQL dosyasına veya loga yazılmaz.
- Offline PasswordHashTool parolayı interaktif console'dan iki kez alır; Spring'i
  veya DB'yi başlatmadan BCrypt cost=12 hash üretir. Hash'i de güvenli saklayın.
  Paketlenmiş jar için komut (Java 21):

  ```sh
  java -Dloader.main=com.fintry.tools.PasswordHashTool \
    -cp target/FinTry-0.0.1-SNAPSHOT.jar \
    org.springframework.boot.loader.launch.PropertiesLauncher
  ```

- Yerel yetkili yönetim aracıyla parametreli transaction kullanın; exact user ID,
  önceki rol ve password_hash=null koşuluyla password_hash + enabled=true
  güncellenir. Etkilenen kayıt sayısı tam 1 olmalı; aksi durumda rollback.
  Kullanıcının rolüne, ID'sine veya finansal kayıtlarına dokunmayın.
- Eski kullanıcının sanal hesabı yoksa kullanıcı girişini açmadan önce ayrı
  doğrulanmış provisioning ile sadece sıfır bakiyeli hesabı oluşturun.
  Mevcut hesap bakiyesi asla sıfırlanmaz.
- İlk giriş ve /auth/me sonucu doğrulanır. Otomatik parola reset/e-posta
  doğrulama API'si yoktur; bu operasyon prosedürüdür.

## İlk ADMIN

Herkese açık register ADMIN oluşturamaz ve self-service rol değiştirme yoktur.
Mevcut ADMIN varsa kimliğini doğrulayarak aynı ID/rol üzerinde yukarıdaki parola
provisioning'i yapın. ADMIN yoksa özel operatörün kontrol ettiği hesabı normal
kayıtla (USER, sıfır bakiye) oluşturun. Ayrı onaylı yerel yönetim transaction'ında
exact ID + beklenen USER koşuluyla ADMIN'e yükseltin; kayıt sayısı tam 1 olmalı.
İşlemi kim, ne zaman, hangi ID için yaptı kaydedilmeli; parola/hash/token audit
loguna konmaz. Yeni girişten sonra yönetim endpoint'i ve diğer kullanıcıların
özel verilerine erişimin reddi doğrulanmalı. Uygulamada bootstrap secret,
varsayılan admin veya otomatik role promotion yoktur.

## Ortam yapılandırması

Kaynak kodunda JWT anahtarı/DB parolası bulunmaz. Runtime'a güvenli secret
mekanizmasıyla şu değişkenler sağlanır; testler bunları izole değerlerle override eder:

| Değişken | Format |
|---|---|
| FINTRY_DB_URL | açık hedef JDBC URL |
| FINTRY_DB_USERNAME | uygulama DB kullanıcısı |
| FINTRY_DB_PASSWORD | secret |
| FINTRY_JWT_PRIVATE_KEY | RSA PKCS8 DER, standard Base64; secret |
| FINTRY_JWT_PUBLIC_KEY | eşleşen RSA X509 DER, standard Base64 |
| FINTRY_FRONTEND_ORIGINS | virgülle ayrılmış exact origin listesi |
| VITE_API_URL | frontend build sırasında API URL |

RSA anahtarları güvenli makinede CSPRNG ile en az 2048 bit üretilir; bu release
anahtar içermez. Anahtar dosyaları ve .env gitignore'dadır. Tek aktif anahtar
çifti vardır; anahtar rotasyonu mevcut token'ları geçersiz kılar. Çoklu anahtar
geçişi/JWKS ayrı kapsamdır. Production HTTPS zorunludur; CSP ve güvenilir reverse
proxy ayarları hosting ortamında uygulanmalıdır.

## Operasyon sınırları

- Access token 15 dakika; refresh yok. Çıkış yalnız istemcidedir. Hesabı kapatmak
  sonraki istekte token kullanımını durdurur; devam eden transaction'ı iptal etmez.
- Auth limiter tek instance belleğindedir, restart'ta sıfırlanır. Çok node ve
  reverse proxy için ortak gateway limiter gereklidir.
- Kaynak dosyada eskiden bulunan DB parolası kaldırılmıştır; git geçmişinden
  silinmiş sayılmaz. Gerçek parola yerelde ayrıca rotate edilmelidir.
- Finansal idempotency eklenmedi; otomatik write retry yapılmaz.
- V3 geri alınırken kullanıcı/finansal verilerini silen down migration yoktur.
  Uyumlu uygulama sürümü veya doğrulanmış yedekten kontrollü restore planlanır.

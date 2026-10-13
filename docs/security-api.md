# Spring Security ve JWT API sözleşmesi

Temel yol mevcut root API'dir; `/api` prefix eklenmedi. İstekler HTTPS üzerinden
`Authorization: Bearer <accessToken>` kullanır. Auth cookie/session yoktur.

## Kayıt ve giriş

- `POST /auth/register`: `{username,email,password}` → 201 UserResponse.
  Rol daima USER; kullanıcı ve sıfır bakiyeli sanal hesap tek transaction'da oluşur.
  Otomatik giriş yapılmaz. Aynı kimlikte çakışma 409; hiçbir yarım kayıt kalmaz.
  Username ve email çakışmaları aynı genel mesajı alır; mevcut kimlik döndürülmez
  veya değiştirilmez. Başarılı 201 ile başarısız 409 ayırt edilebilir: senkron
  kayıt sözleşmesi enumeration'ı tamamen önlemez. İki yolda da BCrypt uygulanır;
  sabit süre garantisi yoktur. Daha sıkı ayrı kayıt bütçeleri sorgulama hızını azaltır.
- `POST /auth/login`: `{email,password}` → 200
  `{accessToken,tokenType:"Bearer",expiresIn:900,user:{id,username,email,role}}`.
  E-posta trim + küçük harfle karşılaştırılır. Geçersiz, parolasız veya kapalı
  hesaplar aynı 401 mesajını alır. Username e-posta yerine giriş alanı değildir.
- `GET /auth/me`: doğrulanmış mevcut kullanıcı özeti.
- Parola en az 12 karakter, en çok 72 UTF-8 byte. BCrypt cost=12.
  Username 3–64 karakter, ASCII harf/rakam/alt çizgi/nokta/tire; email en çok 254.
- Bilinmeyen JSON alanları reddedilir: role, enabled, passwordHash, balance ve
  kullanıcı işlemlerindeki userId gibi alanlar 400'dür.

JWT RS256, 2048 bit veya üstü RSA anahtarlarıyla üretilir. `sub` pozitif kullanıcı
ID'sidir. İmza/algoritma, issuer=`fintry`, audience=`fintry-web`, exp, iat ve nbf
kontrol edilir. Framework'ün timestamp clock skew toleransı 60 saniyedir.
Her istekte kullanıcı DB'den yüklenir; enabled/password_hash kontrol edilir ve
rol DB'den alınır. Token içindeki rol iddiaları kullanılmaz. Rol düşürme ve
hesabı kapatma mevcut token için bir sonraki istekte etkilidir.

## Endpoint izinleri

| Endpoint | USER | ADMIN |
|---|---|---|
| GET /auth/me | Kendi | Kendi |
| GET /accounts/me | Kendi | Kendi |
| GET /portfolio/me | Kendi | Kendi |
| GET /transactions/me | Kendi | Kendi |
| GET /balance-requests/me | Kendi | Kendi |
| GET /accounts/user/{userId} | ID kendi olmalı | ID kendi olmalı |
| GET /portfolio/user/{userId} | ID kendi olmalı | ID kendi olmalı |
| GET /transactions/user/{userId} | ID kendi olmalı | ID kendi olmalı |
| POST /transactions/buy, /transactions/sell | Kendi | Kendi |
| POST /balance-requests | Kendi | Kendi |
| GET /instruments | İzinli | İzinli |
| POST /instruments | 403 | İzinli |
| PUT /instruments/{id}/price?price=... | 403 | İzinli |
| PUT /balance-requests/{id}/approve | 403 | İzinli |
| GET /admin/balance-requests | 403 | Bekleyen talepler |
| GET /users | 403 | Kullanıcı özetleri |
| POST /users | 403 | 403 |
| POST /accounts/user/{userId} | 403 | 403 |

Anonim erişim yalnız POST /auth/register ve POST /auth/login için açıktır.
CORS preflight sadece tanımlı frontend origin'leri için cevaplanır.
Diğer endpoint'ler varsayılan olarak kapalıdır. Başka kullanıcı ID'li erişim,
kaynak varlığını araştırmadan 403 döner; ADMIN sahiplikten muaf değildir.

İşlem gövdesi `{instrumentId,quantity}`, bakiye talebi `{requestedAmount}`.
userId principal'dan alınır. Eski userId gövdeleri uyumsuzdur ve 400 döner.
Finansal yanıt DTO'ları ve BigDecimal hesaplama politikası korunur.
Frontend miktar/tutarı decimal metin olarak gönderir; backend BigDecimal'e çevirir.
JSON yanıtları mevcut sayısal alanları korur; JavaScript'in büyük sayılardaki
hassasiyet sınırı ayrı bir sözleşme değişikliği gerektirir.

## Hatalar ve frontend oturumu

Hata gövdesi `{timestamp,status,error,message}`; 400 girdi, 401 kimlik,
403 yetki/sahiplik, 404 kaynak, 409 çakışma/kilit/işlenmiş talep,
429 giriş/kayıt hız sınırı, 500 beklenmeyen sunucu hatası.
Filter seviyesindeki 401 yanıtı `WWW-Authenticate: Bearer` içerir.
Login aynı çözümlenmiş istemci IP'si için 10 istek/dakika; register ayrı bütçede
5 istek/dakika ve tüm instance için toplam 30 kayıt denemesi/dakika sınırındadır.
Başarılı ve çakışan kayıtlar aynı bütçeyi tüketir; kayıt bütçesi giriş hakkını tüketmez.
Geçersiz JSON/Bean Validation reddi BCrypt öncesindedir ve bu bütçelere dahil değildir.
Kayıt için aggregate limit adres değiştirerek enumeration'ı sınırlar; trafik artışı
veya saldırı sırasında diğer kayıtları da geçici engelleyebilir. Login'e uygulanmaz.
Bellek 10.000 aktif login/register istemci bütçesiyle sınırlıdır; dolunca yeni bütçeler
429 alır. Pencereler 60 saniye, restart'ta sıfırlanır; limitler node'lar arasında paylaşılmaz.

Varsayılan trusted proxy listesi boştur. Güvenilmeyen peer'in X-Forwarded-For ve
Forwarded başlıkları yok sayılır. FINTRY_TRUSTED_PROXIES yalnız exact literal IP
adreslerinden oluşur. Güvenilen peer için X-Forwarded-For sağdan sola değerlendirilir;
ilk güvenilmeyen hop istemcidir, solundaki değerler kimlik seçimini değiştiremez.
Eksik/bozuk/tekrarlanan/aşırı uzun veya tamamen trusted zincir 400 alır; proxy IP'sine
sessiz fallback yapılmaz. Proxy'ler başlığı temizlemeli veya gerçek peer'i sona eklemelidir.
Forwarded alternatif olarak kabul edilmez. Ayrıntılı deployment koşulları security-migration.md'dedir.

DB çakışması/kilit hataları yalnız uygulama hata kodu, doğrulanmış SQLState ve
sunucunun ürettiği korelasyon UUID'siyle loglanır. HTTP handler yanıtındaki
X-Correlation-ID destek kaydını bulmak içindir; kimlik veya SQL içermez ve CORS'ta expose edilir.

Frontend token'ı yalnız bellekte tutar; localStorage/sessionStorage veya cookie
kullanmaz. Yenileme/yeni sekme yeniden giriş gerektirir. Süre dolması ve korumalı
API'den 401 oturumu temizler; 403 yetki hatasıdır. Finansal istekler otomatik tekrar
edilmez. Çıkış istemci oturumunu temizler; sunucuda token süresi dolana kadar
geçerlidir. Refresh token, server-side logout ve parola sıfırlama API'si bu sürümde yoktur.

# Finansal çekirdek: güvenli veritabanı geçişi

Bu rehber migration çalıştırma onayı değildir. Mevcut FinTry ve RiverGuard
veritabanlarında bu geliştirme sırasında hiçbir migration veya test çalıştırılmadı.

## Yerel uygulamanın geçişi

Yeni entity tipleri eski numeric(38,2) şemasıyla birlikte kullanılmamalıdır.
Hibernate schema validation precision/scale ve bütün CHECK kısıtlarını garanti
etmediği için FinancialSchemaVerifier ayrıca salt okunur metadata kontrolü yapar.

Yeni uygulama sürümünde ddl-auto=validate, Flyway enabled=false,
baseline-on-migrate=false ve clean-disabled=true varsayılandır. Uygulama
veritabanını kendi kendine değiştirmez. Eski şemada yeni sürümün başlamaması
beklenen güvenli davranıştır; update/none ile bu koruma aşılmamalıdır.

Yerel çalışmayı kesintisiz sürdürmek için migration onaylanana kadar mevcut
2b0d176 sürümünü kullanın; yeni sürümü yalnız izole Testcontainers testleriyle
çalıştırın. Mevcut veritabanını kullanan çalışan uygulamayı bu sürümle değiştirmeyin.
Yeni sürüme geçiş ve migration aynı bakım penceresinde yapılır. Eski sürümün
ddl-auto=update ayarı da yeni şemada tekrar açılmamalıdır.

## Hassasiyet sözleşmesi

Fiyat, miktar, ortalama maliyet: numeric(44,8).
Bakiye, talep tutarı, işlem toplamı: numeric(52,16).
Her iki grup 36 tam sayı basamağı taşır; eski numeric(38,2) değerlerinin tamamı
bu kapasiteye sığar. BigDecimal girdiler UNNECESSARY ile kontrol edilir; fazla
anlamlı ondalık sessizce yuvarlanmaz. Son sıfırlar kabul edilir.
Fiyat × miktar tam hesaplanır. Ortalama maliyet yalnız sonuçta 8 ondalığa
HALF_EVEN ile yuvarlanır. Birikmiş bakiye ve miktar sınırları ayrıca doğrulanır.
Portföy K/Z negatif olabilir; hesap bakiyesi negatif olamaz.

PostgreSQL doğrudan SQL ile fazla ondalıklı numeric girdisini kolon CHECK
çalışmadan önce yuvarlayabilir. API/service yolu bu girdileri reddeder.
Doğrudan DB yazıcılarının aynı politikaya uyması gerekir.

## Mevcut veritabanı için onaydan sonraki prosedür

1. Hedef sunucu, port, veritabanı, schema ve PostgreSQL sürümünü salt okunur
   doğrulayın. RiverGuard'ın localhost:5432 bağlantısını FinTry olarak varsaymayın.
   Credentials ve bağlantı hedefleri açıkça belirlenmeden işlem yapmayın.
2. Yedek alın ve ayrı ortamda restore testi yapın. Gerçek şemanın kolon,
   null, primary key, unique ve foreign key tanımlarını V1 ile karşılaştırın.
   Hibernate'in FK/unique adları farklı olabilir; anlamları eşleşmelidir.
   Özellikle hesap user_id benzersizliği ve kullanıcı username/email
   benzersizliği bulunmalıdır. Beklenmeyen schema farklarını baseline ile örtmeyin.
3. docs/sql/financial-preflight.sql sorgularını inceleme kopyasında çalıştırın.
   Mükerrer pozisyon, yetim ilişki, null/negatif/sıfır değer veya işlem toplamı
   tutarsızlığı varsa durun. Silme, otomatik birleştirme ve veri düzeltme yoktur.
   Eski miktar yuvarlama kayıpları kendiliğinden geri getirilemez.
4. Tüm eski finansal yazıcıları durdurun. Migration'ı önce yedekten oluşturulan
   ayrı PostgreSQL üzerinde deneyin; süreyi, disk ihtiyacını ve tablo kilitlerini ölçün.
5. Hibernate yönetimindeki doğrulanmış mevcut şemaya Flyway ile açıkça baseline
   version=1 uygulayın. Bu yalnız Flyway geçmişini ekler; V1 DDL çalıştırmaz.
   Boş veritabanında baseline yapılmaz: migrate V1 ve V2'yi çalıştırır.
6. Ayrı migration onayından sonra Flyway CLI ile, açık URL/user/password/schema
   ve filesystem:src/main/resources/db/migration konumunu vererek migrate yapın.
   CLI sürümü POM'un yönettiği Flyway sürümüyle eşleşmeli; PostgreSQL modülü
   bulunmalıdır. Bu rehber mevcut DB için çalıştırılabilir varsayılan hedef içermez.
   baseline-on-migrate kapalı, clean devre dışı kalmalıdır.
7. V2 metadata, kayıt sayıları ve finansal değerlerin öncesi/sonrası farklarını
   kontrol edin. Flyway validate çalıştırın; ardından yeni uygulamayı
   Flyway disabled ve ddl-auto=validate ile başlatın.

V2 tek PostgreSQL transaction'ında tablo kilitleri, preflight, tip genişletmeleri
ve kısıtları uygular. lock_timeout=5s kilit beklemesini sınırlar; migration toplam
süresini sınırlandırmaz. Hata halinde V2 değişiklikleri geri alınır.
Baseline geçmişi ayrı işlemdir ve kalabilir. repair/clean ile hataları gizlemeyin.

## Geri dönüş ve uyumluluk

Kolonları iki ondalığa daraltan down migration yoktur; yeni değerler kaybolabilir.
Geri dönüş, uyumlu uygulama sürümü veya doğrulanmış yedekten kontrollü restore
ile planlanır. Migration sonrası eski yazıcıları otomatik schema update ile
çalıştırmayın. JSON endpoint'leri ve alanları korunmuştur; daha yüksek hassasiyet
yanıtlarda daha çok ondalık üretebilir. Idempotency, auth/JWT ve frontend kapsam dışıdır.

## Test ortamı

Maven testlerinin DB bağlantısı sadece PostgreSqlTestSupport tarafından
başlatılan postgres:16 container'ından gelir. Port dinamiktir, reuse=false,
volume/network/container paylaşımı yoktur. Ryuk sadece Testcontainers'ın
oturum etiketli kaynaklarını temizler; RiverGuard kaynakları yönetilmez.
Docker yoksa testler başarısız olur; mevcut DB'ye fallback veya test atlama yoktur.
Migration testleri aynı geçici container içinde benzersiz schema'lar kullanır.

Komutlar: .\mvnw.cmd test ve .\mvnw.cmd clean verify.

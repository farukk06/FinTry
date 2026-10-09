# Finansal çekirdek geliştirme ve doğrulama raporu

Tarih: 9 Ekim 2026. Başlangıç commit'i: 2b0d176f55437b2e3c0f7eed225599eef453c31d.
Commit, push, PR ve mevcut veritabanına migration yapılmadı.

## Aşama 1 — İzole PostgreSQL

Dosyalar:
- pom.xml: Spring Boot'un yönettiği Testcontainers PostgreSQL 2.0.4 bağımlılığı.
- src/test/java/com/fintry/PostgreSqlTestSupport.java: postgres:16, dinamik port,
  fintry_isolated_test DB, sadece test credentials, reuse=false; Docker yoksa hata.
- src/test/java/com/fintry/FinTryApplicationTests.java: PostgreSQL sürümü,
  veritabanı adı ve dinamik port kontrolü.

Docker Engine 29.7.2 doğrulandı. İlk container PostgreSQL 16.15 ve 56224
portuyla çalıştı; sonraki çalıştırmalarda yeni dinamik portlar ayrıldı.
İlk sandbox Maven denemesi .m2 yazma izni nedeniyle BUILD FAILURE verdi;
testler başlamadı. İzinli çalıştırma başarılı: 1 test, 0 hata, 0 atlanan.
Yerel DB'ye fallback veya RiverGuard container yönetimi yapılmadı.

## Aşama 2 — Hassasiyet ve service doğrulaması

Dosyalar (src/main/java/com/fintry altında):
- service/FinancialPolicy.java, exception/InvalidFinancialValueException.java.
- service/InstrumentService.java, VirtualAccountService.java,
  TransactionService.java, BalanceRequestService.java.
- entity/Instrument.java, VirtualAccount.java, PortfolioAsset.java,
  Transaction.java, BalanceRequest.java: açık precision/scale.

Testler: FinancialPolicyTests.java ve FinancialCoreTests.java eklendi.
Fiyat/miktar numeric(44,8), para numeric(52,16); 36 tam sayı basamağı korunur.
Girdi sessizce yuvarlanmaz; ortalama maliyet HALF_EVEN ile 8 ondalığa yuvarlanır.
0.001 miktarın kalıcı kayıt ve satış sonrası korunması doğrulandı.
Aşama sonucu: 7 test, 0 başarısız, 0 hata, 0 atlanan.

## Aşama 3 — Kilitleme ve atomiklik

Dosyalar (src/main/java/com/fintry altında):
- repository/VirtualAccountRepository.java, BalanceRequestRepository.java.
- service/FinancialLocks.java, TransactionService.java,
  BalanceRequestService.java, VirtualAccountService.java.
- entity/VirtualAccount.java: lazy kullanıcı ilişkisi; kilitli sorguda gereksiz join yok.
- src/test/java/com/fintry/FinancialCoreTests.java: eşzamanlılık/rollback regresyonları.

Kilit sırası: onayda talep -> hesap; alım/satımda hesap -> portföy.
Kilitler PESSIMISTIC_WRITE; PostgreSQL lock_timeout transaction'a özel 5 saniye.
Portföy henüz yokken de hesap kilidi ilk alımları sıraya koyar.
Cloud 1/900 -> iki alım -> 3/700 örneği, ortak bakiye, fazla satış,
karma alım/satım, tek/ayrı talep onayları ve geçmiş yazımında rollback doğrulandı.
Aşama sonucu: 16 test, 0 başarısız, 0 hata, 0 atlanan.

## Aşama 4 — Migration ve bütünlük

Dosyalar:
- pom.xml: Spring Boot Flyway starter ve PostgreSQL modülü.
- src/main/resources/db/migration/V1__legacy_schema.sql.
- src/main/resources/db/migration/V2__financial_integrity.sql.
- src/main/resources/application.properties: validate, Flyway kapalı,
  otomatik baseline kapalı, clean kapalı.
- src/main/java/com/fintry/config/FinancialSchemaVerifier.java:
  numeric precision/scale ve gerekli kısıtların salt okunur başlangıç kontrolü.
- entity/PortfolioAsset.java, Transaction.java, BalanceRequest.java;
  service/BalanceRequestService.java: unique, zorunlu alanlar ve kullanıcı ilişkisi.
- src/test/java/com/fintry/PostgreSqlTestSupport.java: izole DB'de Flyway + validate.
- src/test/java/com/fintry/MigrationTests.java ve FinancialCoreTests.java.
- docs/financial-core-migration.md, docs/sql/financial-preflight.sql.

Boş kurulum, mevcut şemada açık baseline gereksinimi, eski değerlerin korunması,
mükerrer/yetim/bozuk kayıtta migration reddi, DB check/FK/unique kısıtları test edildi.
Aşama sonucu: 27 test, 0 başarısız, 0 hata, 0 atlanan.
V2 hesap unique kısıtını uk_virtual_account_user olarak adlandırır; indeks/veri
yeniden oluşturulmaz. Bu son düzenleme beşinci aşamanın tam testlerinde doğrulandı.

Mevcut veritabanı migration'ı ayrıca onay gerektirir. Yeni sürüm eski şemada
bilerek başlamaz; geçiş onayına kadar eski uygulama sürümü kullanılmalıdır.
Yeni sürüm update/none ile eski şemada çalıştırılmamalıdır. Ayrıntı migration rehberinde.

## Aşama 5 — HTTP, son regresyonlar ve derleme

Dosyalar (src/main/java/com/fintry altında):
- validation/FinancialPrecision.java, FinancialPrecisionValidator.java.
- dto/TradeRequest.java, CreateInstrumentRequest.java, CreateBalanceRequest.java.
- controller/InstrumentController.java, VirtualAccountController.java.
- exception/GlobalExceptionHandler.java: 400/404/409 sözleşmesi; beklenmeyen
  DB hatası 500 olarak kalır, istemciye SQL ayrıntısı verilmez.
- service/PortfolioService.java, FinancialPolicy.java: aynı hassasiyet politikası,
  büyük pozitif/negatif üslerde tahsis öncesi kontrol.

Test dosyaları: FinancialHttpTests.java eklendi; FinancialCoreTests.java,
FinancialPolicyTests.java ve MigrationTests.java genişletildi.
HTTP hata gövdesi, gerçek 5 saniyelik kilit timeout'u, paralel hesap oluşturma,
8/16 ondalık kayıtlar, eski bozuk fiyatla alım/satımın reddi, kontrollü talep
kilidi yarışı ve sekiz bozuk eski veri migration senaryosu doğrulandı.

Son komutlar:
- .\mvnw.cmd -B test: BUILD SUCCESS; 45 test, 0 başarısız, 0 hata, 0 atlanan.
- .\mvnw.cmd -B clean verify: BUILD SUCCESS; 45 test, 0 başarısız, 0 hata, 0 atlanan;
  target/FinTry-0.0.1-SNAPSHOT.jar üretildi.
- git diff --check: başarılı; Windows LF/CRLF bilgilendirmeleri var.

Son test dağılımı:
| Sınıf | Test |
|---|---:|
| FinancialCoreTests | 17 |
| FinancialHttpTests | 7 |
| FinancialPolicyTests | 4 |
| FinTryApplicationTests | 1 |
| MigrationTests | 16 |

Migration reddi, duplicate key ve lock_timeout senaryolarındaki beklenen SQL
hataları loglarda görünür; testler bu hataları ve değişikliklerin geri alınmasını
doğrular. Başarısız test gizlenmedi, atlanmadı; Surefire raporları target/surefire-reports altında.

Frontend, Spring Security/JWT, idempotency ve mevcut hatalı verilerin otomatik
onarımı kapsam dışıdır. Mevcut FinTry/RiverGuard DB'lerine bağlanılmadı.

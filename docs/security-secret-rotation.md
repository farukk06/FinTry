# Eski PostgreSQL secret için ayrı yerel operasyon

Bu liste yürütülmüş işlem veya veritabanına erişim onayı değildir. Eski secret
burada tekrar edilmez; Git geçmişi rewrite edilmez. Kaynaktan kaldırılması rotasyon değildir.

1. Yetkili operatör doğru server/port/database/schema ve uygulama DB rolünü güvenilir
   yönetim kanalında doğrular; bağımlı uygulamaları ve rollback/maintenance planını belirler.
2. Yeni güçlü secret güvenli secret manager'da üretilir. CLI argümanı, shell history,
   SQL dosyası, ticket, kaynak kod veya loga yazılmaz. Yönetim aracının parola gizleme
   ve statement/parameter logging davranışı önce doğrulanır.
3. Doğrulanmış rolün parolası yetkili yerel yönetim aracında ayrı onayla değiştirilir.
   Uygulama FINTRY_DB_PASSWORD secret'ı güncellenir; havuzdaki eski bağlantılar
   kontrollü drain/restart edilir. Parola değişimi mevcut DB session'larını tek başına iptal etmez.
4. Yeni secret ile yeni bağlantı açılarak current_database/current_user ve bağlantı
   hedefi doğrulanır; küçük read-only sağlık sorgusu çalıştırılır. Otomatik migration,
   ddl-auto update veya finansal yazma yapılmaz.
5. Eski credential ile yeni bağlantının reddedildiği güvenli, log redaksiyonu açık
   ortamda doğrulanır. Sağlık kontrolü ve uygulama auth/okuma akışı kontrol edilir;
   pool auth hataları yalnız güvenli kod/korelasyonla takip edilir.
6. Hedef/rol, operatör, zaman, secret sürümü ve doğrulama sonuçları kaydedilir;
   eski/yeni secret değerleri kaydedilmez. Aynı secret başka yerde kullanılmışsa
   oradaki rotasyon da ayrı planlanır. Geçmiş log/artifact erişimi ve retention incelenir.

Bu görev gerçek parola değiştirmez, gerçek fintry_db'ye bağlanmaz ve history rewrite yapmaz.

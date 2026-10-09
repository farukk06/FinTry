# FinTry frontend

Node 22+ ile `npm ci`, `npm run dev`, `npm test`, `npm run lint`, `npm run build`.
`VITE_API_URL` build-time API adresidir; local geliştirme varsayılanı
http://localhost:8080. .env.example secret içermez.

Giriş/kayıt ekranları /login ve /register. Piyasa, portföy ve işlem sayfaları
oturum ister; /admin ADMIN ister. Backend izinleri güvenlik otoritesidir.
Oturum bellektedir; reload/new tab yeniden giriş gerektirir. Token expiry ve
401 oturumu temizler; cookie/localStorage/sessionStorage kullanılmaz.
Finansal işlemler otomatik retry edilmez. API ve migration ayrıntıları:
../docs/security-api.md ve ../docs/security-migration.md.

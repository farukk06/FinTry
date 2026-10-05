import { useEffect, useState } from "react";

function Dashboard() {

    return (
        <>
            <section className="market-summary">
                <div className="section-head">
                    <h1>Piyasa Özeti</h1>
                    <span>Veriler örnek amaçlıdır.</span>
                </div>

                <div className="market-summary-grid">
                    <div className="market-box">
                        <h3>BIST 100</h3>
                        <p>12.665,53</p>
                        <span className="loss">▼ -%1,10</span>
                    </div>

                    <div className="market-box">
                        <h3>USD / TRY</h3>
                        <p>43,25</p>
                        <span className="profit">▲ +%0,40</span>
                    </div>

                    <div className="market-box">
                        <h3>EUR / TRY</h3>
                        <p>50,80</p>
                        <span className="profit">▲ +%0,20</span>
                    </div>

                    <div className="market-box">
                        <h3>ONS ALTIN</h3>
                        <p>4.876 $</p>
                        <span className="profit">▲ +%2,25</span>
                    </div>
                </div>
            </section>

            <section className="card quick-access">
                <h2>Hızlı Erişim</h2>

                <div className="quick-grid">
                    <div className="quick-item">Hisse Ara</div>
                    <div className="quick-item">Döviz Çevirici</div>
                    <div className="quick-item">Portföy Oluştur</div>
                    <div className="quick-item">Sanal Alım-Satım</div>
                    <div className="quick-item">En Çok Yükselenler</div>
                    <div className="quick-item">En Çok Düşenler</div>
                </div>
            </section>

            <section className="card news-preview">
                <h2>Güncel Finans Haberleri</h2>

                <div className="news-grid">
                    <article className="news-card">
                        <div className="news-image"></div>
                        <h3>Borsada Günün Gelişmeleri</h3>
                        <p>Borsa İstanbul’da günün öne çıkan başlıkları ve sektör bazlı hareketler...</p>
                        <span>Devamını Oku →</span>
                    </article>

                    <article className="news-card">
                        <div className="news-image"></div>
                        <h3>Dolar/TL’de Son Durum</h3>
                        <p>Kur tarafında gün içi hareketlilik ve piyasa beklentileri yakından izleniyor...</p>
                        <span>Devamını Oku →</span>
                    </article>

                    <article className="news-card">
                        <div className="news-image"></div>
                        <h3>Altın Fiyatlarında Artış</h3>
                        <p>Ons altın tarafında yükseliş eğilimi ve güvenli liman talebi dikkat çekiyor...</p>
                        <span>Devamını Oku →</span>
                    </article>
                </div>
            </section>
        </>
    );
}

export default Dashboard;
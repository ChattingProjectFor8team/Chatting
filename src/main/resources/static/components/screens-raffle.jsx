// Connectfin — Raffle: list + detail + entry flow

const RAFFLES = [
  {
    id: 1, artistId: 1, title: 'LUMEN8 DM 30일권', category: 'DM_PASS',
    prize: 'YUNA의 1:1 DM 30일 무제한', winners: 5,
    cost: 3, endsAt: '2h 14m',
    entries: 12_482, maxWeight: 6, hot: true,
    desc: 'LUMEN8 멤버 YUNA와의 DM 구독권을 30일간 무료로 사용하세요. 당첨자에게는 YUNA의 음성 인사말이 포함됩니다.',
    rules: ['Fan Membership 가입 필요', 'Jelly 3개 차감 (미당첨 시 전액 환불)', '당첨자 발표 후 48시간 내 지급', '양도 불가']
  },
  {
    id: 2, artistId: 3, title: 'NOIR7 영상통화 팬미팅', category: 'VIDEO_CALL',
    prize: '1:1 영상통화 3분 × 30명', winners: 30,
    cost: 5, endsAt: '1d 6h',
    entries: 48_291, maxWeight: 6, hot: true,
    desc: 'NOIR7 전 멤버와 랜덤으로 매칭되는 영상통화. 사전 예약된 시간에 진행되며 녹화/캡처는 불가능합니다.',
    rules: ['DM Subscription 가입자 우선', 'Jelly 5개 차감', '국내/해외 IP 구분 후 추첨', '실명 인증 필수']
  },
  {
    id: 3, artistId: 1, title: '싸인 폴라로이드 세트', category: 'GOODS',
    prize: 'LUMEN8 멤버 5종 싸인 폴라로이드', winners: 50,
    cost: 2, endsAt: '3d 2h',
    entries: 24_112, maxWeight: 4,
    desc: '공식 앨범 "PRISM" 발매 기념 한정 폴라로이드. 당첨자 주소로 배송됩니다.',
    rules: ['배송지 국내 한정', '중복 응모 불가', '양도 불가']
  },
  {
    id: 4, artistId: 4, title: 'hanabi* 사운드체크 초대', category: 'EVENT',
    prize: '8/17 쇼케이스 사운드체크 참관 + 미니 MD', winners: 20,
    cost: 4, endsAt: '5d 18h',
    entries: 8_402, maxWeight: 3,
    desc: '쇼케이스 당일 오후 5시, 사운드체크를 참관하고 hanabi*와 짧게 인사할 수 있는 기회입니다.',
    rules: ['공연 티켓 별도 구매 필요', '신분증 지참', '동반인 불가']
  },
  {
    id: 5, artistId: 2, title: 'Kagerō 친필 엽서', category: 'GOODS',
    prize: '멤버 4인 친필 엽서 (랜덤)', winners: 100,
    cost: 1, endsAt: '6d',
    entries: 3_241, maxWeight: 2,
    desc: '투어 파이널 기념. 멤버별 친필 엽서가 랜덤으로 한 장씩 발송됩니다.',
    rules: ['국제 배송 가능 (배송비 팬 부담)', '중복 응모 불가']
  },
  {
    id: 6, artistId: 6, title: 'ORBITAL 스튜디오 라이브 초대', category: 'EVENT',
    prize: '비공개 스튜디오 라이브 + 애프터 토크', winners: 10,
    cost: 6, endsAt: '종료됨', ended: true,
    entries: 52_012, maxWeight: 6,
    desc: '10/5 비공개 스튜디오 라이브. (종료)',
    rules: []
  },
];

function mapRaffle(r) {
  const endTime = r.startedAt && r.durationMinutes
    ? new Date(new Date(r.startedAt).getTime() + r.durationMinutes * 60000)
    : null;
  const now = new Date();
  const ended = r.status === 'COMPLETED' || r.status === 'CANCELLED';
  const remaining = endTime && !ended ? endTime - now : 0;

  let endsAt = '종료됨';
  if (!ended && remaining > 0) {
    const h = Math.floor(remaining / 3600000);
    const m = Math.floor((remaining % 3600000) / 60000);
    if (h >= 24) endsAt = `${Math.floor(h / 24)}d ${h % 24}h`;
    else endsAt = `${h}h ${m}m`;
  }

  return {
    id: r.id,
    artistId: r.artistId,
    title: r.title,
    category: r.rewardType || 'GENERAL',
    prize: r.title,
    winners: r.totalWinners,
    cost: 0, // API에 cost 필드 없음
    endsAt,
    entries: 0,
    maxWeight: 1,
    hot: false,
    ended,
    desc: '',
    rules: [],
    entryCondition: r.entryCondition,
    status: r.status,
  };
}

function RaffleListScreen({ t, onBack, onOpenRaffle }) {
  const [filter, setFilter] = React.useState('all');
  const [raffles, setRaffles] = React.useState(RAFFLES);

  React.useEffect(() => {
    // 모든 아티스트의 래플을 한번에 로드
    Promise.all(
      ARTISTS.map(a =>
        window.ConnectfinAPI.api(`/api/v1/artists/${a.id}/raffles`)
          .then(data => (data || []).map(r => ({ ...mapRaffle(r), artistId: a.id })))
          .catch(err => { console.warn('API batch item failed:', err?.message || err); return []; })
      )
    ).then(results => {
      const all = results.flat();
      if (all.length > 0) setRaffles(all);
    });
  }, []);

  const filtered = filter === 'all' ? raffles.filter(r => !r.ended)
    : filter === 'ended' ? raffles.filter(r => r.ended)
    : raffles.filter(r => r.category === filter);

  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingTop: 60, paddingBottom: 96 }}>
      <div style={{ padding: '0 20px 8px', display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <div style={{ fontFamily: t.fontDisplay, fontSize: 28, fontWeight: 800, letterSpacing: -0.8 }}>Raffle</div>
          <div style={{ fontSize: 12, color: t.textDim, fontFamily: t.fontMono, marginTop: 2 }}>
            🍮 142 Jelly · 당첨 가중치 × 2
          </div>
        </div>
        <div style={{
          padding: '5px 10px', borderRadius: 8, background: t.chip,
          color: t.accent, fontSize: 10, fontFamily: t.fontMono, fontWeight: 700, letterSpacing: 0.5,
        }}>DM+ TIER</div>
      </div>

      {/* Hero featured */}
      {(() => {
        const hero = RAFFLES.find(r => r.hot);
        const ha = ARTISTS.find(x => x.id === hero.artistId);
        return (
          <div onClick={() => onOpenRaffle(hero.id)} style={{
            margin: '0 16px 16px', borderRadius: 20, overflow: 'hidden',
            cursor: 'pointer', position: 'relative', height: 200,
            background: `repeating-linear-gradient(120deg, ${ha.color1} 0 28px, ${ha.color2} 28px 56px)`,
          }}>
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(0,0,0,0.15) 0%, rgba(0,0,0,0.85) 100%)' }}/>
            <div style={{
              position: 'absolute', top: 14, left: 14,
              padding: '4px 9px', borderRadius: 6, background: t.liveGradient, color: '#fff',
              fontSize: 10, fontWeight: 800, letterSpacing: 0.5, display: 'flex', gap: 5, alignItems: 'center',
            }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#fff', animation: 'connectfin-pulse 1.2s ease-in-out infinite' }}/>
              HOT · ENDS IN {hero.endsAt}
            </div>
            <div style={{ position: 'absolute', bottom: 14, left: 16, right: 16, color: '#fff' }}>
              <div style={{ fontSize: 11, fontFamily: t.fontMono, opacity: 0.85, letterSpacing: 0.5 }}>
                {ha.name} · RAFFLE
              </div>
              <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, marginTop: 2, lineHeight: 1.2 }}>
                {hero.title}
              </div>
              <div style={{ display: 'flex', gap: 14, marginTop: 8, fontSize: 11, fontFamily: t.fontMono, opacity: 0.9 }}>
                <span>👥 {hero.entries.toLocaleString()} 응모</span>
                <span>🏆 {hero.winners}명 당첨</span>
                <span>🍮 {hero.cost} Jelly</span>
              </div>
            </div>
          </div>
        );
      })()}

      {/* Filter chips */}
      <div style={{ display: 'flex', gap: 6, padding: '0 16px 12px', overflowX: 'auto' }}>
        {[
          { k: 'all', label: 'ALL' },
          { k: 'DM_PASS', label: 'DM PASS' },
          { k: 'VIDEO_CALL', label: '영상통화' },
          { k: 'EVENT', label: '오프라인' },
          { k: 'GOODS', label: '굿즈' },
          { k: 'ended', label: '종료됨' },
        ].map(f => (
          <button key={f.k} onClick={() => setFilter(f.k)} style={{
            flexShrink: 0, padding: '6px 12px', borderRadius: 20,
            background: filter === f.k ? t.gradient : 'transparent',
            color: filter === f.k ? '#fff' : t.textDim,
            border: filter === f.k ? 'none' : `1px solid ${t.line}`,
            fontSize: 11, fontWeight: 700, cursor: 'pointer', fontFamily: t.fontMono, letterSpacing: 0.5,
          }}>{f.label}</button>
        ))}
      </div>

      {/* List */}
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {filtered.map(r => {
          const a = ARTISTS.find(x => x.id === r.artistId);
          const pct = Math.min(100, (r.entries / 60000) * 100);
          return (
            <button key={r.id} onClick={() => onOpenRaffle(r.id)} style={{
              textAlign: 'left', cursor: 'pointer', padding: 12,
              borderRadius: 16, background: t.surface, border: `1px solid ${t.line}`,
              display: 'flex', gap: 12, alignItems: 'center',
              opacity: r.ended ? 0.55 : 1,
              fontFamily: t.font, color: t.text,
            }}>
              <div style={{
                width: 72, height: 72, borderRadius: 12, flexShrink: 0,
                background: `repeating-linear-gradient(45deg, ${a.color1} 0 14px, ${a.color2} 14px 28px)`,
                position: 'relative', overflow: 'hidden',
              }}>
                <div style={{
                  position: 'absolute', bottom: 0, left: 0, right: 0,
                  padding: '3px 6px', background: 'rgba(0,0,0,0.55)',
                  fontSize: 9, fontFamily: t.fontMono, color: '#fff', letterSpacing: 0.5,
                }}>{r.category.replace('_', ' ')}</div>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                  <span style={{ fontSize: 11, color: t.textDim, fontWeight: 600 }}>{a.name}</span>
                  {r.hot && !r.ended && <span style={{ fontSize: 9, fontWeight: 800, padding: '1px 5px', borderRadius: 3, background: t.hot, color: '#fff' }}>HOT</span>}
                </div>
                <div style={{ fontWeight: 700, fontSize: 14, marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {r.title}
                </div>
                <div style={{ marginTop: 6, height: 4, background: t.surface2, borderRadius: 2, overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${pct}%`, background: t.gradient }}/>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, fontSize: 10, color: t.textMuted, fontFamily: t.fontMono }}>
                  <span>{r.entries.toLocaleString()} 응모 · {r.winners}명 당첨</span>
                  <span style={{ color: r.ended ? t.textMuted : t.accent, fontWeight: 700 }}>
                    {r.ended ? '종료' : `⏳ ${r.endsAt}`}
                  </span>
                </div>
              </div>
            </button>
          );
        })}
      </div>

      <div style={{ padding: '20px 20px 0', fontSize: 10, color: t.textMuted, fontFamily: t.fontMono, textAlign: 'center', lineHeight: 1.6 }}>
        GET /raffles?cursor=...&status=active<br/>
        가중치 계산: base × membership_tier × jelly_spent
      </div>
    </div>
  );
}

function RaffleDetailScreen({ t, theme, raffleId, onBack, onEnter }) {
  const mockR = RAFFLES.find(x => x.id === raffleId) || RAFFLES[0];
  const [r, setR] = React.useState(mockR);
  const [myEntry, setMyEntry] = React.useState(null);
  const a = ARTISTS.find(x => x.id === r.artistId);
  const [weight, setWeight] = React.useState(1);
  const [agreed, setAgreed] = React.useState(false);
  const [entering, setEntering] = React.useState(false);
  const [result, setResult] = React.useState(null); // 'submitted' | 'error'
  const totalCost = r.cost * weight;
  const myBalance = 142;

  React.useEffect(() => {
    if (!r.artistId) return;
    // 래플 상세
    window.ConnectfinAPI.api(`/api/v1/artists/${r.artistId}/raffles/${raffleId}`)
      .then(data => setR(prev => ({ ...prev, ...mapRaffle(data), entered: data.entered })))
      .catch(err => { console.warn('API error suppressed:', err?.message || err); });
    // 내 응모 결과
    if (window.ConnectfinAPI.getToken()) {
      window.ConnectfinAPI.api(`/api/v1/artists/${r.artistId}/raffles/${raffleId}/entries/me`)
        .then(data => setMyEntry(data))
        .catch(err => { console.warn('API error suppressed:', err?.message || err); });
    }
  }, [raffleId]);

  const submit = async () => {
    if (r.ended || !agreed || entering) return;
    if (!window.ConnectfinAPI.getToken()) return;
    setEntering(true);
    try {
      await window.ConnectfinAPI.api(`/api/v1/artists/${r.artistId}/raffles/${raffleId}/entries`, { method: 'POST' });
      setResult('submitted');
    } catch (err) {
      setResult('error');
    } finally {
      setEntering(false);
    }
  };

  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingBottom: 120, background: t.bg }}>
      {/* Cover */}
      <div style={{ position: 'relative', height: 240 }}>
        <div style={{
          position: 'absolute', inset: 0,
          background: `repeating-linear-gradient(30deg, ${a.color1} 0 28px, ${a.color2} 28px 56px)`,
        }}/>
        <div style={{ position: 'absolute', inset: 0, background: `linear-gradient(180deg, rgba(0,0,0,0.2) 0%, ${t.bg} 100%)` }}/>
        <button onClick={onBack} style={{
          position: 'absolute', top: 60, left: 16, width: 36, height: 36, borderRadius: '50%',
          background: 'rgba(0,0,0,0.5)', color: '#fff', border: 'none', fontSize: 18, cursor: 'pointer',
          backdropFilter: 'blur(10px)',
        }}>←</button>
        {!r.ended && (
          <div style={{
            position: 'absolute', top: 60, right: 16,
            padding: '6px 12px', borderRadius: 8, background: t.liveGradient, color: '#fff',
            fontSize: 11, fontWeight: 800, letterSpacing: 0.5, display: 'flex', gap: 6, alignItems: 'center',
            backdropFilter: 'blur(10px)',
          }}>
            <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#fff', animation: 'connectfin-pulse 1.2s ease-in-out infinite' }}/>
            ENDS IN {r.endsAt}
          </div>
        )}
        <div style={{ position: 'absolute', bottom: 14, left: 18, right: 18, color: '#fff' }}>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <ArtistAvatar artist={a} size={36} t={t}/>
            <div>
              <div style={{ fontWeight: 700, fontSize: 14 }}>{a.name}</div>
              <div style={{ fontSize: 10, fontFamily: t.fontMono, opacity: 0.8, letterSpacing: 0.5 }}>{r.category.replace('_', ' ')} · RAFFLE</div>
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: '20px 20px 0' }}>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 26, fontWeight: 800, letterSpacing: -0.7, lineHeight: 1.2 }}>
          {r.title}
        </div>
        <div style={{
          marginTop: 10, padding: 14, borderRadius: 14,
          background: t.surface, border: `1px solid ${t.line}`,
        }}>
          <div style={{ fontSize: 10, color: t.accent, fontFamily: t.fontMono, fontWeight: 700, letterSpacing: 0.5 }}>🏆 PRIZE</div>
          <div style={{ fontSize: 14, fontWeight: 700, marginTop: 4, color: t.text }}>{r.prize}</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, marginTop: 12 }}>
            {[
              { k: '당첨', v: `${r.winners}명` },
              { k: '응모', v: r.entries.toLocaleString() },
              { k: '비용', v: `🍮 ${r.cost}` },
            ].map(s => (
              <div key={s.k}>
                <div style={{ fontSize: 10, color: t.textMuted, fontFamily: t.fontMono, letterSpacing: 0.5 }}>{s.k}</div>
                <div style={{ fontWeight: 800, fontSize: 15, color: t.text, marginTop: 1 }}>{s.v}</div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 16, fontSize: 13, lineHeight: 1.6, color: t.textDim }}>
          {r.desc}
        </div>

        {/* Rules */}
        {r.rules.length > 0 && (
          <div style={{ marginTop: 16 }}>
            <div style={{ fontSize: 11, fontFamily: t.fontMono, fontWeight: 700, letterSpacing: 0.5, color: t.textDim, marginBottom: 8 }}>
              RULES
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {r.rules.map((rule, i) => (
                <div key={i} style={{ display: 'flex', gap: 8, fontSize: 12.5, color: t.text, lineHeight: 1.5 }}>
                  <span style={{ color: t.accent, fontWeight: 800, flexShrink: 0 }}>{String(i+1).padStart(2,'0')}</span>
                  <span>{rule}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Weight picker */}
        {!r.ended && (
          <div style={{ marginTop: 20, padding: 14, borderRadius: 14, background: t.surface, border: `1px solid ${t.line}` }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
              <div style={{ fontSize: 12, fontWeight: 700, color: t.text }}>당첨 확률 × <span style={{ color: t.accent, fontSize: 16, fontFamily: t.fontDisplay }}>{weight}</span></div>
              <div style={{ fontSize: 10, fontFamily: t.fontMono, color: t.textMuted }}>MAX {r.maxWeight}</div>
            </div>
            <input type="range" min="1" max={r.maxWeight} step="1" value={weight}
              onChange={e => setWeight(+e.target.value)}
              style={{ width: '100%', accentColor: t.accent }}/>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6, fontSize: 10, fontFamily: t.fontMono, color: t.textMuted }}>
              <span>× 1 (기본)</span>
              <span>× {r.maxWeight} (최대)</span>
            </div>
            <div style={{
              marginTop: 12, padding: '10px 12px', borderRadius: 10, background: t.surface2,
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            }}>
              <span style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>
                {r.cost} × {weight} = <b style={{ color: t.text }}>{totalCost}</b> Jelly 차감
              </span>
              <span style={{ fontSize: 10, color: t.textMuted, fontFamily: t.fontMono }}>
                잔액 {myBalance} 🍮
              </span>
            </div>
          </div>
        )}

        {/* Agreement */}
        {!r.ended && (
          <label style={{
            marginTop: 14, display: 'flex', gap: 10, padding: 12,
            borderRadius: 12, background: t.surface2, cursor: 'pointer',
            alignItems: 'center',
          }}>
            <div style={{
              width: 18, height: 18, borderRadius: 5,
              border: `2px solid ${agreed ? t.accent : t.lineStrong}`,
              background: agreed ? t.accent : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', fontSize: 11, fontWeight: 800, flexShrink: 0,
            }}>{agreed ? '✓' : ''}</div>
            <input type="checkbox" checked={agreed} onChange={e => setAgreed(e.target.checked)} style={{ display: 'none' }}/>
            <span style={{ fontSize: 12, color: t.text, lineHeight: 1.5 }}>
              응모 규정 및 개인정보 수집에 동의합니다. (미당첨 시 Jelly 전액 자동 환불)
            </span>
          </label>
        )}
      </div>

      {/* Sticky CTA */}
      {!r.ended && (
        <div style={{
          position: 'absolute', bottom: 0, left: 0, right: 0,
          padding: '14px 18px 34px',
          background: `linear-gradient(180deg, transparent, ${t.bg} 40%)`,
        }}>
          <button onClick={submit} disabled={!agreed || totalCost > myBalance || entering} style={{
            width: '100%', padding: '16px', borderRadius: 14, border: 'none',
            background: (!agreed || totalCost > myBalance) ? t.surface2 : t.gradient,
            color: (!agreed || totalCost > myBalance) ? t.textMuted : '#fff',
            fontWeight: 800, fontSize: 15, cursor: (!agreed || totalCost > myBalance) ? 'not-allowed' : 'pointer',
            fontFamily: t.font, letterSpacing: -0.2,
            boxShadow: agreed && totalCost <= myBalance ? `0 10px 30px ${t.accent}33` : 'none',
            transition: 'all 0.2s',
          }}>
            {entering ? '응모 중…' :
             totalCost > myBalance ? `잔액 부족 · ${totalCost - myBalance} Jelly 더 필요` :
             `🎰 ${totalCost} Jelly로 응모 · 확률 × ${weight}`}
          </button>
        </div>
      )}

      {r.ended && (
        <div style={{
          margin: '20px 20px 0', padding: 14, borderRadius: 12, background: t.surface2,
          textAlign: 'center', fontSize: 12, color: t.textDim, fontFamily: t.fontMono,
        }}>
          이 래플은 종료되었습니다 · 당첨자 개별 통보 완료
        </div>
      )}

      {/* Success modal */}
      {result === 'submitted' && (
        <div style={{
          position: 'absolute', inset: 0, zIndex: 90,
          background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          padding: 24,
        }} onClick={() => { setResult(null); onEnter && onEnter(); }}>
          <div onClick={e => e.stopPropagation()} style={{
            width: '100%', maxWidth: 320, padding: 24, borderRadius: 22,
            background: t.surface, border: `1px solid ${t.lineStrong}`,
            textAlign: 'center',
          }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%', margin: '0 auto',
              background: t.gradient, display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 36, boxShadow: `0 10px 40px ${t.accent}55`,
            }}>🎉</div>
            <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 20, marginTop: 14, color: t.text }}>
              응모 완료!
            </div>
            <div style={{ fontSize: 13, color: t.textDim, lineHeight: 1.55, marginTop: 8 }}>
              <b style={{ color: t.text }}>{r.title}</b> 응모가 접수되었어요.<br/>
              당첨자는 <b style={{ color: t.accent }}>{r.endsAt}</b> 후 발표돼요.
            </div>
            <div style={{
              marginTop: 14, padding: '8px 12px', borderRadius: 10, background: t.surface2,
              fontSize: 11, color: t.textMuted, fontFamily: t.fontMono, letterSpacing: 0.3,
            }}>
              entry_id · rf_{r.id}_{Math.random().toString(36).slice(2,10)}<br/>
              weight × {weight} · {totalCost} Jelly escrow
            </div>
            <button onClick={() => { setResult(null); onBack(); }} style={{
              marginTop: 16, width: '100%', padding: 12, borderRadius: 12, border: 'none',
              background: t.gradient, color: '#fff', fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
            }}>목록으로 돌아가기</button>
          </div>
        </div>
      )}
    </div>
  );
}

Object.assign(window, { RAFFLES, RaffleListScreen, RaffleDetailScreen });

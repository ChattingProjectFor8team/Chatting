// Connectfin — Home, Artist, Media, Community, Membership, Login screens

function HomeScreen({ t, theme, speed, liveOn, onNav, onOpenLive, onOpenArtist }) {
  const liveArtists = ARTISTS.filter(a => a.live && liveOn);
  const [viewers, setViewers] = React.useState(liveArtists.map(a => a.viewers));
  const [dashboard, setDashboard] = React.useState(null);

  React.useEffect(() => {
    if (!liveOn) return;
    const id = setInterval(() => {
      setViewers(prev => prev.map((v, i) => Math.max(1000, v + Math.round((Math.random() - 0.4) * 120 * speed))));
    }, 900 / speed);
    return () => clearInterval(id);
  }, [speed, liveOn]);

  React.useEffect(() => {
    if (!window.ConnectfinAPI.getToken()) return;
    window.ConnectfinAPI.api('/api/member/v1/home/dashboard')
      .then(data => setDashboard(data))
      .catch(() => {});
  }, []);

  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingTop: 54, paddingBottom: 96 }}>
      {/* Header */}
      <div style={{ padding: '14px 20px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Infinity8 size={36} color={t.accent} color2={t.accent2} stroke={7}/>
          <span style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 22, letterSpacing: -0.6 }}>
            Connectfin
          </span>
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center', color: t.textDim }}>
          <span style={{ fontSize: 20 }}>⌕</span>
          <span style={{ fontSize: 20, position: 'relative' }}>
            ⚑
            <span style={{ position: 'absolute', top: -2, right: -4, width: 8, height: 8, borderRadius: '50%', background: t.hot }}/>
          </span>
        </div>
      </div>

      {/* Following strip */}
      <div style={{ padding: '6px 0 10px', overflowX: 'auto' }}>
        <div style={{ display: 'flex', gap: 14, padding: '0 20px' }}>
          {ARTISTS.map(a => (
            <button key={a.id} onClick={() => onOpenArtist(a.id)} style={{
              background: 'transparent', border: 'none', padding: 0, cursor: 'pointer',
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6,
              flexShrink: 0,
            }}>
              <ArtistAvatar artist={a} size={56} live={a.live && liveOn} ring t={t}/>
              <span style={{ fontSize: 11, color: t.textDim, fontWeight: 600, maxWidth: 62, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {a.name}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Live carousel */}
      {liveArtists.length > 0 && (
        <div style={{ padding: '8px 20px 20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: t.hot, boxShadow: `0 0 10px ${t.hot}` }}/>
              <span style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 18, letterSpacing: -0.4 }}>LIVE NOW</span>
            </div>
            <span style={{ fontSize: 12, color: t.textDim, fontFamily: t.fontMono }}>{liveArtists.length} streams</span>
          </div>
          <div style={{ display: 'flex', gap: 12, overflowX: 'auto', scrollSnapType: 'x mandatory', margin: '0 -20px', padding: '0 20px' }}>
            {liveArtists.map((a, i) => (
              <div key={a.id} onClick={() => onOpenLive(a.id)} style={{
                flexShrink: 0, width: 260, cursor: 'pointer', scrollSnapAlign: 'start',
              }}>
                <MediaPlaceholder artist={a} kind="LIVE" label={`${a.stage} — Live`} aspect="16/11" t={t}/>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <ArtistAvatar artist={a} size={28} t={t}/>
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 700 }}>{a.stage}</div>
                      <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono }}>
                        👁 {viewers[i]?.toLocaleString() || '—'}
                      </div>
                    </div>
                  </div>
                  <span style={{
                    fontSize: 10, fontWeight: 800, padding: '3px 8px', borderRadius: 6,
                    background: t.liveGradient, color: '#fff', letterSpacing: 0.5,
                  }}>LIVE</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Feed */}
      <div style={{ padding: '0 20px' }}>
        <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 18, letterSpacing: -0.4, marginBottom: 10 }}>
          Today's Feed
        </div>

        {/* 대시보드 API 로드 시 — 구독 아티스트 최신 글 */}
        {dashboard?.subscribedArtistsLatestPosts?.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            {dashboard.subscribedArtistsLatestPosts.map(item => (
              <div key={item.artist.artistId} style={{ marginBottom: 14 }}>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8,
                  fontSize: 12, fontWeight: 700, color: t.textDim,
                }}>
                  <span style={{ fontSize: 14 }}>⭐</span>
                  {item.artist.name}의 최신 소식
                </div>
                {item.posts.map(post => {
                  const a = ARTISTS.find(x => x.id === item.artist.artistId) || { name: item.artist.name, id: item.artist.artistId, color1: t.accent, color2: t.accent2 };
                  return (
                    <div key={post.artistPostId} onClick={() => onOpenArtist(item.artist.artistId)} style={{
                      background: t.surface, borderRadius: 20, padding: 16, marginBottom: 10,
                      border: `1px solid ${t.accent}40`, cursor: 'pointer',
                    }}>
                      <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 10 }}>
                        <ArtistAvatar artist={a} size={40} t={t}/>
                        <div style={{ flex: 1 }}>
                          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                            <span style={{ fontWeight: 700, fontSize: 14 }}>{post.writerNickname}</span>
                            {post.artistBadge && (
                              <span style={{ fontSize: 9, fontWeight: 800, padding: '2px 6px', borderRadius: 4, background: t.gradient, color: '#fff' }}>ARTIST ✓</span>
                            )}
                          </div>
                          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>
                            {window.ConnectfinAPI.formatTime(post.createdAt)}
                          </div>
                        </div>
                      </div>
                      <div style={{ fontSize: 14, lineHeight: 1.55, color: t.text }}>{post.content}</div>
                      <div style={{ display: 'flex', gap: 18, marginTop: 12, fontSize: 12, color: t.textDim }}>
                        <span>♡ {window.ConnectfinAPI.formatCount(post.likeCount)}</span>
                        <span>💬 {post.commentCount}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        )}

        {/* 대시보드 API 로드 시 — 팔로우 멤버 최신 글 */}
        {dashboard?.followedArtistMembersLatestPosts?.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: t.textDim, marginBottom: 10 }}>팔로우 멤버 소식</div>
            {dashboard.followedArtistMembersLatestPosts.map(item => {
              if (!item.post) return null;
              const a = ARTISTS.find(x => x.id === item.artist.artistId) || { name: item.artist.name, id: item.artist.artistId, color1: t.accent, color2: t.accent2 };
              return (
                <div key={item.artistMemberId} onClick={() => onOpenArtist(item.artist.artistId)} style={{
                  background: t.surface, borderRadius: 20, padding: 16, marginBottom: 10,
                  border: `1px solid ${t.line}`, cursor: 'pointer',
                }}>
                  <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 10 }}>
                    <ArtistAvatar artist={a} size={40} t={t}/>
                    <div style={{ flex: 1 }}>
                      <span style={{ fontWeight: 700, fontSize: 14 }}>{item.stageName}</span>
                      <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>
                        {item.artist.name} · {window.ConnectfinAPI.formatTime(item.post.createdAt)}
                      </div>
                    </div>
                  </div>
                  <div style={{ fontSize: 14, lineHeight: 1.55, color: t.text }}>{item.post.content}</div>
                  <div style={{ display: 'flex', gap: 18, marginTop: 12, fontSize: 12, color: t.textDim }}>
                    <span>♡ {window.ConnectfinAPI.formatCount(item.post.likeCount)}</span>
                    <span>💬 {item.post.commentCount}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* 기존 mock 피드 (대시보드가 없거나 비로그인) */}
        {!dashboard && FEED_POSTS.map(post => {
          const a = ARTISTS.find(x => x.id === post.artistId);
          const isArtist = post.type === 'artist';
          return (
            <div key={post.id} style={{
              background: t.surface, borderRadius: 20, padding: 16, marginBottom: 12,
              border: isArtist ? `1px solid ${t.accent}40` : `1px solid ${t.line}`,
              position: 'relative', overflow: 'hidden',
            }}>
              {post.pinned && (
                <div style={{
                  position: 'absolute', top: 0, right: 0,
                  fontSize: 9, fontWeight: 800, padding: '3px 10px',
                  background: t.gradient, color: '#fff',
                  borderBottomLeftRadius: 10, letterSpacing: 0.5,
                  fontFamily: t.fontMono,
                }}>📌 PINNED</div>
              )}
              <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 10 }}>
                <ArtistAvatar artist={a} size={40} t={t}/>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                    <span style={{ fontWeight: 700, fontSize: 14 }}>{post.author}</span>
                    {isArtist && (
                      <span style={{
                        fontSize: 9, fontWeight: 800,
                        padding: '2px 6px', borderRadius: 4,
                        background: t.gradient, color: '#fff', letterSpacing: 0.4,
                      }}>ARTIST ✓</span>
                    )}
                  </div>
                  <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>
                    {post.authorRole} · {post.time}
                  </div>
                </div>
                <span style={{ color: t.textMuted }}>⋯</span>
              </div>
              <div style={{ fontSize: 14, lineHeight: 1.55, marginBottom: 12, color: t.text }}>
                {post.body}
              </div>
              {post.id === 101 && <MediaPlaceholder artist={a} kind="IMAGE" label="백스테이지 컷 · 3장" aspect="4/3" t={t}/>}
              <div style={{ display: 'flex', gap: 18, marginTop: 12, fontSize: 12, color: t.textDim }}>
                <span>♡ {post.likes.toLocaleString()}</span>
                <span>💬 {post.comments.toLocaleString()}</span>
                <span style={{ marginLeft: 'auto', color: t.accent, fontWeight: 600 }}>#{a.name}</span>
              </div>
            </div>
          );
        })}
        <div style={{ textAlign: 'center', padding: 20, color: t.textMuted, fontFamily: t.fontMono, fontSize: 11 }}>
          — {dashboard ? 'Dashboard loaded' : 'Cursor: id < 101'} —
        </div>
      </div>
    </div>
  );
}

function ArtistScreen({ t, theme, artistId, onBack, onOpenLive, onOpenDM }) {
  const a = ARTISTS.find(x => x.id === artistId) || ARTISTS[0];
  const [tab, setTab] = React.useState('feed');
  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingBottom: 96 }}>
      {/* Cover */}
      <div style={{ position: 'relative', height: 240 }}>
        <div style={{
          position: 'absolute', inset: 0,
          background: `repeating-linear-gradient(45deg, ${a.color1} 0 22px, ${a.color2} 22px 44px)`,
        }}/>
        <div style={{
          position: 'absolute', inset: 0,
          background: `linear-gradient(180deg, rgba(0,0,0,0.25) 0%, ${t.bg} 100%)`,
        }}/>
        <button onClick={onBack} style={{
          position: 'absolute', top: 60, left: 16,
          width: 36, height: 36, borderRadius: '50%',
          background: 'rgba(0,0,0,0.5)', color: '#fff',
          border: 'none', fontSize: 18, cursor: 'pointer', backdropFilter: 'blur(10px)',
        }}>←</button>
        <div style={{ position: 'absolute', top: 60, right: 16, display: 'flex', gap: 8 }}>
          <div style={{
            padding: '6px 12px', borderRadius: 20,
            background: 'rgba(0,0,0,0.5)', color: '#fff',
            fontSize: 11, fontFamily: t.fontMono, backdropFilter: 'blur(10px)',
          }}>👥 {a.followers}</div>
        </div>
      </div>

      {/* Profile */}
      <div style={{ padding: '0 20px', marginTop: -60, position: 'relative' }}>
        <ArtistAvatar artist={a} size={104} ring t={t}/>
        <div style={{ marginTop: 14 }}>
          <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 32, letterSpacing: -1 }}>
            {a.name} {a.live && <span style={{
              fontSize: 11, fontWeight: 800, padding: '4px 10px',
              background: t.liveGradient, color: '#fff', borderRadius: 6,
              marginLeft: 8, verticalAlign: 'middle', letterSpacing: 0.5,
            }}>● LIVE</span>}
          </div>
          <div style={{ fontSize: 13, color: t.textDim, fontFamily: t.fontMono, marginTop: 2 }}>
            {a.stage} · {a.genre} · {a.members} members
          </div>
          <div style={{ fontSize: 14, lineHeight: 1.6, marginTop: 12, color: t.text }}>
            매일의 순간을 무한히 연결하는 공식 공간. 새 앨범, 라이브, 비하인드 — 모든 소식은 여기서.
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button style={{
              flex: 1, padding: '12px 0', borderRadius: 14, border: 'none',
              background: t.gradient, color: '#fff', fontWeight: 800, fontSize: 14, cursor: 'pointer',
              fontFamily: t.font,
            }}>FOLLOWING ✓</button>
            <button onClick={() => onOpenDM(a.id)} style={{
              padding: '12px 20px', borderRadius: 14, border: `1px solid ${t.lineStrong}`,
              background: t.surface, color: t.text, fontWeight: 700, fontSize: 14, cursor: 'pointer',
              fontFamily: t.font,
            }}>💌 DM</button>
            {a.live && (
              <button onClick={() => onOpenLive(a.id)} style={{
                padding: '12px 20px', borderRadius: 14, border: 'none',
                background: t.liveGradient, color: '#fff', fontWeight: 800, fontSize: 14, cursor: 'pointer',
                fontFamily: t.font,
              }}>▶ LIVE</button>
            )}
          </div>
        </div>

        {/* Membership card */}
        <div style={{
          marginTop: 18, borderRadius: 18, padding: 16,
          background: t.gradient, color: '#fff', position: 'relative', overflow: 'hidden',
        }}>
          <div style={{ position: 'absolute', right: -20, top: -20, opacity: 0.2 }}>
            <Infinity8 size={140} color="#fff" color2="#fff" stroke={14}/>
          </div>
          <div style={{ fontSize: 11, fontFamily: t.fontMono, opacity: 0.85, letterSpacing: 0.5 }}>FAN MEMBERSHIP</div>
          <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, marginTop: 4 }}>
            {a.name} Fan Club
          </div>
          <div style={{ fontSize: 12, marginTop: 4, opacity: 0.9 }}>팬레터 · 전용 DM방 · 래플 우선 응모</div>
          <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 18 }}>
              9 <span style={{ fontSize: 11, opacity: 0.8 }}>JELLY / 월</span>
            </div>
            <button style={{
              padding: '8px 16px', borderRadius: 10, border: 'none',
              background: 'rgba(255,255,255,0.22)', color: '#fff', fontWeight: 700, fontSize: 12, cursor: 'pointer',
              backdropFilter: 'blur(10px)',
            }}>가입하기</button>
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: 24, marginTop: 20, borderBottom: `1px solid ${t.line}` }}>
          {['feed', 'artist', 'media', 'live'].map(k => (
            <button key={k} onClick={() => setTab(k)} style={{
              background: 'transparent', border: 'none', padding: '10px 0',
              color: tab === k ? t.text : t.textDim,
              fontWeight: tab === k ? 800 : 600, fontSize: 14, cursor: 'pointer',
              borderBottom: tab === k ? `2px solid ${t.accent}` : '2px solid transparent',
              fontFamily: t.font, letterSpacing: -0.2,
            }}>
              {k === 'feed' ? 'Feed' : k === 'artist' ? 'Artist' : k === 'media' ? 'Media' : 'Live'}
            </button>
          ))}
        </div>

        {/* Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 4, marginTop: 12 }}>
          {Array.from({ length: 9 }, (_, i) => (
            <div key={i} style={{ aspectRatio: '1/1', background:
              `repeating-linear-gradient(${i*20}deg, ${a.color1} 0 10px, ${a.color2} 10px 20px)`,
              borderRadius: 6, position: 'relative', overflow: 'hidden',
            }}>
              <div style={{
                position: 'absolute', bottom: 4, right: 6,
                fontFamily: t.fontMono, fontSize: 9, color: '#fff',
                background: 'rgba(0,0,0,0.5)', padding: '1px 4px', borderRadius: 3,
              }}>{i+1}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function MediaScreen({ t, onBack }) {
  const [filter, setFilter] = React.useState('all');
  const filtered = filter === 'all' ? MEDIA : MEDIA.filter(m => m.kind === filter);
  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingTop: 60, paddingBottom: 96 }}>
      <div style={{ padding: '0 20px 12px' }}>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 28, fontWeight: 800, letterSpacing: -0.8 }}>Media</div>
        <div style={{ fontSize: 13, color: t.textDim, marginTop: 2 }}>공식 MV · 비하인드 · 팬캠까지</div>
      </div>
      <div style={{ display: 'flex', gap: 8, padding: '0 20px 16px', overflowX: 'auto' }}>
        {['all','video','photo'].map(k => (
          <button key={k} onClick={() => setFilter(k)} style={{
            padding: '6px 14px', borderRadius: 20,
            background: filter === k ? t.gradient : 'transparent',
            color: filter === k ? '#fff' : t.textDim,
            border: filter === k ? 'none' : `1px solid ${t.line}`,
            fontSize: 12, fontWeight: 700, cursor: 'pointer', fontFamily: t.font,
            textTransform: 'uppercase', letterSpacing: 0.5,
          }}>{k}</button>
        ))}
      </div>
      <div style={{ padding: '0 20px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {filtered.map(m => {
          const a = ARTISTS.find(x => x.id === m.artistId);
          return (
            <div key={m.id}>
              <MediaPlaceholder artist={a} kind={m.kind} label={m.title} aspect={m.kind === 'video' ? '3/4' : '1/1'} t={t}/>
              <div style={{ marginTop: 6, fontSize: 12, fontWeight: 700, lineHeight: 1.3 }}>
                {m.title}
              </div>
              <div style={{ fontSize: 10, color: t.textMuted, fontFamily: t.fontMono, marginTop: 2 }}>
                {m.duration ? `${m.duration} · ${m.views} views` : `${m.daysAgo}d ago`}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function MembershipScreen({ t, onOpenRaffles }) {
  const tiers = [
    { name: 'Free', price: '0', subs: ['팬 포스트 열람', '아티스트 포스트 리액션'], color: t.surface2, text: t.text },
    { name: 'Fan Membership', price: '9', subs: ['팬레터 작성 ✍', '멤버 전용 포스트', '래플 기본 응모'], color: t.gradient, text: '#fff', hot: true },
    { name: 'DM Subscription', price: '15', subs: ['1:1 DM 송수신 💬', 'Fan Membership 포함', '래플 가중치 ×2'], color: t.liveGradient, text: '#fff' },
  ];
  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingTop: 60, paddingBottom: 96 }}>
      <div style={{ padding: '0 20px 16px' }}>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 28, fontWeight: 800, letterSpacing: -0.8 }}>Membership</div>
        <div style={{ fontSize: 13, color: t.textDim, marginTop: 2 }}>젤리로 결제 · 1 Jelly = 300 KRW</div>
      </div>

      {/* Wallet */}
      <div style={{ margin: '0 20px 20px', borderRadius: 20, padding: 16, background: t.surface, border: `1px solid ${t.line}` }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono, letterSpacing: 0.5 }}>JELLY WALLET</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 4 }}>
              <span style={{ fontFamily: t.fontDisplay, fontSize: 32, fontWeight: 800, background: t.gradient, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                142
              </span>
              <span style={{ fontSize: 14, color: t.textDim, fontWeight: 700 }}>🍮 Jelly</span>
            </div>
            <div style={{ fontSize: 11, color: t.textMuted, fontFamily: t.fontMono }}>≈ 42,600 KRW · Auto-charge ON</div>
          </div>
          <button style={{
            padding: '10px 18px', borderRadius: 12, border: 'none',
            background: t.gradient, color: '#fff', fontWeight: 800, fontSize: 13, cursor: 'pointer',
          }}>+ 충전</button>
        </div>
      </div>

      <div style={{ padding: '0 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        {tiers.map(tier => (
          <div key={tier.name} style={{
            background: tier.color, color: tier.text, borderRadius: 20, padding: 18,
            position: 'relative', overflow: 'hidden',
            border: tier.name === 'Free' ? `1px solid ${t.line}` : 'none',
          }}>
            {tier.hot && (
              <div style={{
                position: 'absolute', top: 12, right: 12,
                fontSize: 9, fontWeight: 800, padding: '3px 8px', borderRadius: 4,
                background: 'rgba(255,255,255,0.22)', color: '#fff', letterSpacing: 0.5,
              }}>POPULAR</div>
            )}
            <div style={{ fontFamily: t.fontDisplay, fontSize: 20, fontWeight: 800, letterSpacing: -0.4 }}>{tier.name}</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 4 }}>
              <span style={{ fontFamily: t.fontDisplay, fontSize: 30, fontWeight: 800 }}>{tier.price}</span>
              <span style={{ fontSize: 11, opacity: 0.85 }}>JELLY/월</span>
            </div>
            <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 6 }}>
              {tier.subs.map(s => (
                <div key={s} style={{ fontSize: 13 }}>✓ {s}</div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Raffle promo */}
      <div style={{ margin: '20px 20px 0', padding: 16, borderRadius: 18, background: t.surface, border: `1px dashed ${t.accent}` }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontSize: 11, color: t.accent, fontFamily: t.fontMono, fontWeight: 700, letterSpacing: 0.5 }}>🎰 RAFFLE</div>
            <div style={{ fontWeight: 800, fontSize: 15, marginTop: 2 }}>LUMEN8 DM 30일권</div>
            <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>진행중 · 남은시간 02:14:58 · 당첨 5명</div>
          </div>
          <button onClick={onOpenRaffles} style={{
            padding: '8px 14px', borderRadius: 10, border: `1px solid ${t.accent}`,
            background: 'transparent', color: t.accent, fontWeight: 800, fontSize: 12, cursor: 'pointer',
          }}>응모</button>
        </div>
      </div>
    </div>
  );
}

function LoginScreen({ t, onLogin }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', padding: '100px 28px 40px' }}>
      <div style={{ marginBottom: 40 }}>
        <Infinity8 size={72} color={t.accent} color2={t.accent2} stroke={7}/>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 40, fontWeight: 800, letterSpacing: -1.2, marginTop: 16 }}>
          Connectfin
        </div>
        <div style={{ fontSize: 14, color: t.textDim, marginTop: 6, lineHeight: 1.5 }}>
          무한히 연결되는 팬덤 경험.<br/>아티스트와 당신 사이, 단 한 번의 탭.
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <input placeholder="이메일" style={{
          padding: '14px 16px', borderRadius: 14, border: `1px solid ${t.lineStrong}`,
          background: t.surface, color: t.text, fontSize: 14, fontFamily: t.font,
        }}/>
        <input placeholder="비밀번호" type="password" style={{
          padding: '14px 16px', borderRadius: 14, border: `1px solid ${t.lineStrong}`,
          background: t.surface, color: t.text, fontSize: 14, fontFamily: t.font,
        }}/>
        <button onClick={onLogin} style={{
          padding: '14px', borderRadius: 14, border: 'none',
          background: t.gradient, color: '#fff', fontWeight: 800, fontSize: 15, cursor: 'pointer',
          fontFamily: t.font, marginTop: 8,
        }}>로그인 →</button>
      </div>
      <div style={{ marginTop: 20, display: 'flex', gap: 12, alignItems: 'center' }}>
        <div style={{ flex: 1, height: 1, background: t.line }}/>
        <span style={{ fontSize: 11, color: t.textMuted, fontFamily: t.fontMono }}>OR</span>
        <div style={{ flex: 1, height: 1, background: t.line }}/>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 16 }}>
        {['카카오로 시작','Apple로 시작','Google로 시작'].map(l => (
          <button key={l} style={{
            padding: '12px', borderRadius: 14, border: `1px solid ${t.line}`,
            background: t.surface, color: t.text, fontWeight: 600, fontSize: 13, cursor: 'pointer',
            fontFamily: t.font,
          }}>{l}</button>
        ))}
      </div>
      <div style={{ marginTop: 'auto', textAlign: 'center', fontSize: 11, color: t.textMuted, fontFamily: t.fontMono }}>
        아직 계정이 없나요? <span style={{ color: t.accent, fontWeight: 700 }}>회원가입</span>
      </div>
    </div>
  );
}

Object.assign(window, { HomeScreen, ArtistScreen, MediaScreen, MembershipScreen, LoginScreen });

// Connectfin — Desktop artist profile (8 tabs)

// Artist cover hero (top banner with gradient)
function ArtistCoverBanner({ artist, t, theme }) {
  return (
    <div style={{
      height: 180, position: 'relative', overflow: 'hidden',
      borderBottom: `1px solid ${t.line}`,
      background: `repeating-linear-gradient(135deg, ${artist.color1} 0 40px, ${artist.color2} 40px 80px)`,
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(180deg, rgba(0,0,0,0.15) 0%, rgba(0,0,0,0.45) 100%)',
      }}/>
      <div style={{
        position: 'absolute', bottom: 16, left: 24, color: '#fff',
      }}>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 28, fontWeight: 800, letterSpacing: -0.5 }}>{artist.name}.</div>
        <div style={{ fontSize: 12, opacity: 0.85, fontFamily: t.fontMono, letterSpacing: 0.3 }}>
          가입하고 최신 소식을 받아보세요!
        </div>
      </div>
      <button style={{
        position: 'absolute', bottom: 20, right: 24,
        padding: '8px 16px', borderRadius: 10, border: 'none',
        background: '#fff', color: '#111', fontWeight: 800, fontSize: 13, cursor: 'pointer',
      }}>가입하기</button>
    </div>
  );
}

// Main desktop profile router
function DesktopArtistProfile({ t, theme, artist, tab, onNavProfile, onOpenLive, onOpenDM, onOpenMembership }) {
  let body;
  if (tab === 'highlight') body = <TabHighlight t={t} theme={theme} artist={artist} onNavProfile={onNavProfile}/>;
  else if (tab === 'fan') body = <TabFan t={t} theme={theme} artist={artist}/>;
  else if (tab === 'artist') body = <TabArtistPosts t={t} theme={theme} artist={artist}/>;
  else if (tab === 'fanletter') body = <TabFanLetter t={t} theme={theme} artist={artist}/>;
  else if (tab === 'media') body = <TabMedia t={t} theme={theme} artist={artist}/>;
  else if (tab === 'live') body = <TabLive t={t} theme={theme} artist={artist} onOpenLive={onOpenLive}/>;
  else if (tab === 'notice') body = <TabNotice t={t} theme={theme} artist={artist}/>;
  else if (tab === 'shop') body = <TabShop t={t} theme={theme} artist={artist}/>;

  return (
    <div>
      <ArtistCoverBanner artist={artist} t={t} theme={theme}/>
      <div style={{ display: 'flex', maxWidth: 1320, margin: '0 auto', padding: '20px 24px 60px', gap: 24 }}>
        <div style={{ flex: 1, minWidth: 0 }}>{body}</div>
        <DesktopRightSidebar t={t} theme={theme} artist={artist} onOpenDM={onOpenDM} onOpenMembership={onOpenMembership}/>
      </div>
    </div>
  );
}

// ────────── HIGHLIGHT (summary) ──────────
function TabHighlight({ t, theme, artist, onNavProfile }) {
  const notices = NOTICES.filter(n => n.artistId === artist.id).slice(0, 5);
  const artistPost = ARTIST_POSTS.find(p => p.artistId === artist.id);
  const fanPosts = FAN_POSTS.filter(p => p.artistId === artist.id).slice(2, 8);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {/* Notice preview */}
      <Section t={t} theme={theme}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
          <span style={{ fontSize: 16 }}>📢</span>
          <div style={{ fontWeight: 800, fontSize: 15 }}>Notice</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {notices.map(n => (
            <div key={n.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
              <span style={{ color: t.textDim }}>·</span>
              <span style={{ flex: 1 }}>{n.title}</span>
              {n.pinned && <span style={{ fontSize: 10, color: t.hot }}>📍</span>}
            </div>
          ))}
        </div>
      </Section>

      {/* From artist */}
      <Section t={t} theme={theme}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ fontWeight: 800, fontSize: 15 }}>From {artist.name}.</div>
          <button onClick={() => onNavProfile('artist')} style={linkBtn(t)}>더 보기 →</button>
        </div>
        {artistPost && <ArtistPostCard post={artistPost} artist={artist} t={t} inline/>}
      </Section>

      {/* Fan posts grid */}
      <Section t={t} theme={theme}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ fontWeight: 800, fontSize: 15 }}>Fan Posts</div>
          <button onClick={() => onNavProfile('fan')} style={linkBtn(t)}>더 보기 →</button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          {fanPosts.map(p => <FanPostMiniCard key={p.id} post={p} artist={artist} t={t}/>)}
        </div>
      </Section>
    </div>
  );
}

// ────────── FAN (팬 포스트 타임라인) ──────────
function TabFan({ t, theme, artist }) {
  const posts = FAN_POSTS.filter(p => p.artistId === artist.id);
  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 14 }}>
        <Chip t={t} active>전체</Chip>
        <Chip t={t}><span>🔥</span> Hot</Chip>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {posts.map(p => <FanPostFullCard key={p.id} post={p} artist={artist} t={t} theme={theme}/>)}
      </div>
    </div>
  );
}

// ────────── ARTIST (아티스트 포스트 타임라인) ──────────
function TabArtistPosts({ t, theme, artist }) {
  const posts = ARTIST_POSTS.filter(p => p.artistId === artist.id);
  return (
    <div>
      {/* Moments (hero horizontal) */}
      <Section t={t} theme={theme} style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 800, fontSize: 15, marginBottom: 12 }}>Moments</div>
        <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4 }}>
          {HIGHLIGHTS.filter(h => h.artistId === artist.id).slice(0, 6).map(h => (
            <div key={h.id} style={{
              flexShrink: 0, width: 120, height: 180, borderRadius: 14, overflow: 'hidden', position: 'relative',
              background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
              cursor: 'pointer',
            }}>
              <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, transparent 50%, rgba(0,0,0,0.6))' }}/>
              <div style={{ position: 'absolute', bottom: 6, left: 8, color: '#fff', fontSize: 11, fontWeight: 700 }}>{artist.name}</div>
              <div style={{ position: 'absolute', top: 6, left: 8, fontSize: 18 }}>{h.emoji}</div>
            </div>
          ))}
        </div>
      </Section>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {posts.map(p => <ArtistPostCard key={p.id} post={p} artist={artist} t={t}/>)}
      </div>
    </div>
  );
}

// ────────── FAN LETTER (텍스처 카드 그리드) ──────────
function TabFanLetter({ t, theme, artist }) {
  const letters = FAN_LETTERS.filter(l => l.artistId === artist.id);
  const [selected, setSelected] = React.useState(null);

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 14 }}>
        <Chip t={t} active>전체</Chip>
        <Chip t={t}><span>🔥</span> Hot</Chip>
        <div style={{ flex: 1 }}/>
        <Chip t={t}>🌐 한국어</Chip>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        {letters.map(l => <FanLetterCard key={l.id} letter={l} artist={artist} t={t} theme={theme} onClick={() => setSelected(l)}/>)}
      </div>

      {selected && <FanLetterModal letter={selected} artist={artist} t={t} theme={theme} onClose={() => setSelected(null)}/>}
    </div>
  );
}

function FanLetterCard({ letter, artist, t, theme, onClick }) {
  const dark = theme === 'dark';
  const texture = (() => {
    if (letter.texture === 'notepad') {
      return {
        background: '#F8F2E4',
        backgroundImage: `repeating-linear-gradient(180deg, transparent 0 32px, rgba(120,100,70,0.22) 32px 33px)`,
        color: '#3a2f1f',
        fontFamily: 'ui-monospace, "Courier New", monospace',
      };
    }
    if (letter.texture === 'blackboard') {
      return {
        background: '#1b2820',
        color: '#f4e8c1',
        fontFamily: '"Space Grotesk", serif',
      };
    }
    if (letter.texture === 'hearts') {
      return {
        background: `linear-gradient(180deg, #f0e6ff 0%, #e0d0ff 100%)`,
        backgroundImage: `radial-gradient(circle at 20% 30%, rgba(255,255,255,0.9) 6px, transparent 7px), radial-gradient(circle at 70% 70%, rgba(255,255,255,0.85) 8px, transparent 9px), radial-gradient(circle at 85% 20%, rgba(255,255,255,0.8) 5px, transparent 6px), radial-gradient(circle at 30% 85%, rgba(255,255,255,0.9) 7px, transparent 8px), linear-gradient(180deg, #f0e6ff 0%, #e0d0ff 100%)`,
        color: '#4a2d6e',
      };
    }
    if (letter.texture === 'balloon') {
      return {
        background: 'linear-gradient(135deg, #ffeef6 0%, #fde5f0 100%)',
        color: '#8B2D5C',
        fontFamily: t.fontDisplay,
        fontWeight: 800,
      };
    }
    if (letter.texture === 'grid') {
      return {
        background: '#fff9ef',
        backgroundImage: `linear-gradient(rgba(180,140,80,0.2) 1px, transparent 1px), linear-gradient(90deg, rgba(180,140,80,0.2) 1px, transparent 1px)`,
        backgroundSize: '20px 20px',
        color: '#4a3820',
      };
    }
    return { background: '#fff', color: '#111' };
  })();

  return (
    <div onClick={onClick} style={{
      cursor: 'pointer', borderRadius: 12, overflow: 'hidden',
      border: `1px solid ${t.line}`, background: dark ? '#141624' : t.surface,
    }}>
      <div style={{
        aspectRatio: '3/4', padding: '20px 18px', position: 'relative',
        ...texture,
        fontSize: 11, lineHeight: 1.6, whiteSpace: 'pre-line',
        overflow: 'hidden',
      }}>
        {letter.body}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 14px', fontSize: 12 }}>
        <ArtistAvatar artist={artist} size={20} t={t}/>
        <span style={{ color: t.textDim }}>To. {artist.name}</span>
        <span style={{ marginLeft: 'auto', color: t.textDim, fontFamily: t.fontMono, fontSize: 10 }}>· {letter.author}</span>
      </div>
    </div>
  );
}

function FanLetterModal({ letter, artist, t, theme, onClose }) {
  return (
    <div onClick={onClose} style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 200,
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 30,
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        width: 480, maxHeight: '85vh', overflowY: 'auto',
        background: theme === 'dark' ? '#141624' : t.surface,
        borderRadius: 16, border: `1px solid ${t.lineStrong}`,
        color: t.text, padding: 28,
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <ArtistAvatar artist={artist} size={32} t={t}/>
            <div>
              <div style={{ fontWeight: 800, fontSize: 14 }}>To. {artist.name}</div>
              <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>From. {letter.author}</div>
            </div>
          </div>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: t.text, fontSize: 22, cursor: 'pointer' }}>×</button>
        </div>
        <div style={{ whiteSpace: 'pre-line', lineHeight: 1.7, fontSize: 14 }}>
          {letter.body}
        </div>
        <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
          <button style={{ flex: 1, padding: '10px 0', borderRadius: 10, border: `1px solid ${t.line}`, background: 'transparent', color: t.text, cursor: 'pointer', fontWeight: 700, fontSize: 12 }}>♡ 공감하기</button>
          <button style={{ flex: 1, padding: '10px 0', borderRadius: 10, border: 'none', background: t.gradient, color: '#fff', cursor: 'pointer', fontWeight: 800, fontSize: 12 }}>🌐 번역 보기</button>
        </div>
      </div>
    </div>
  );
}

// ────────── MEDIA (최신 + 멤버십 2단) ──────────
function TabMedia({ t, theme, artist }) {
  const latest = MEDIA_EXTENDED.filter(m => m.artistId === artist.id).slice(0, 6);
  const membership = MEDIA_EXTENDED.filter(m => m.artistId === artist.id && m.membership).slice(0, 4);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div style={{ display: 'flex', gap: 6 }}>
        <Chip t={t} active><span style={{ fontSize: 10 }}>🏠</span></Chip>
        <Chip t={t}>멤버십</Chip>
        <Chip t={t}>전체</Chip>
        <div style={{ flex: 1 }}/>
        <Chip t={t}>⇅ 필터</Chip>
      </div>

      <Section t={t} theme={theme}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 14 }}>최신</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14 }}>
          {latest.map(m => <MediaCard key={m.id} media={m} artist={artist} t={t}/>)}
        </div>
        <button style={moreBtn(t)}>더 보기</button>
      </Section>

      <Section t={t} theme={theme}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
          <span style={{ color: t.accent2 }}>✦</span>
          <div style={{ fontWeight: 800, fontSize: 14 }}>멤버십</div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div style={{ fontSize: 12, color: t.textDim }}>최신</div>
          <button style={linkBtn(t)}>더 보기 →</button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
          {membership.map(m => <MediaCard key={m.id} media={m} artist={artist} t={t} small/>)}
        </div>
        <div style={{
          marginTop: 14, padding: '12px', textAlign: 'center',
          border: `1px solid ${t.line}`, borderRadius: 10, fontSize: 12, color: t.textDim,
        }}>
          지금 멤버십에 가입하고 혜택 누리기
          <button style={{ ...moreBtn(t), marginTop: 8 }}>멤버십 가입하기</button>
        </div>
      </Section>
    </div>
  );
}

function MediaCard({ media, artist, t, small }) {
  return (
    <div style={{ cursor: 'pointer', fontFamily: t.font }}>
      <div style={{ position: 'relative', borderRadius: 10, overflow: 'hidden', aspectRatio: '16/10', background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})` }}>
        {media.membership && (
          <div style={{ position: 'absolute', top: 8, left: 8, padding: '3px 6px', borderRadius: 4, background: t.accent2, color: '#fff', fontSize: 10, fontWeight: 800 }}>
            M
          </div>
        )}
        <div style={{ position: 'absolute', bottom: 8, right: 8, padding: '2px 6px', borderRadius: 4, background: 'rgba(0,0,0,0.7)', color: '#fff', fontSize: 10, fontFamily: t.fontMono }}>
          {media.duration}
        </div>
      </div>
      <div style={{ marginTop: 8, fontSize: small ? 12 : 13, fontWeight: 600, lineHeight: 1.4, color: t.text, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
        {media.title}
      </div>
      {media.isNew && <div style={{ display: 'inline-block', marginTop: 4, padding: '1px 6px', background: t.hot, color: '#fff', fontSize: 9, fontWeight: 800, borderRadius: 3 }}>NEW</div>}
      <div style={{ fontSize: 11, color: t.textDim, marginTop: 2, fontFamily: t.fontMono }}>{media.date}</div>
    </div>
  );
}

// ────────── LIVE (리플레이 그리드) ──────────
function TabLive({ t, theme, artist, onOpenLive }) {
  const replays = LIVE_REPLAYS.filter(r => r.artistId === artist.id);
  return (
    <div>
      <Section t={t} theme={theme}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div style={{ fontWeight: 800, fontSize: 15 }}>LIVE Replay</div>
          <div style={{ display: 'flex', gap: 8, fontSize: 11, color: t.textDim }}>
            <span>최신순 ▾</span>
            <span>전체 연도 ▾</span>
          </div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 14 }}>
          {replays.map(r => <LiveReplayCard key={r.id} replay={r} artist={artist} t={t} onOpen={() => onOpenLive(artist.id)}/>)}
        </div>
      </Section>
    </div>
  );
}

function LiveReplayCard({ replay, artist, t, onOpen }) {
  return (
    <div onClick={onOpen} style={{ cursor: 'pointer' }}>
      <div style={{
        position: 'relative', aspectRatio: '16/10', borderRadius: 10, overflow: 'hidden',
        background: replay.voiceOnly
          ? `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`
          : `repeating-linear-gradient(45deg, ${artist.color1} 0 20px, ${artist.color2} 20px 40px)`,
      }}>
        {replay.membership && (
          <div style={{ position: 'absolute', top: 8, left: 8, padding: '3px 6px', borderRadius: 4, background: t.accent2, color: '#fff', fontSize: 10, fontWeight: 800 }}>M</div>
        )}
        {replay.voiceOnly && (
          <div style={{
            position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexDirection: 'column', gap: 8, color: 'rgba(255,255,255,0.8)',
          }}>
            <span style={{ fontSize: 40 }}>🎙</span>
            <span style={{ fontSize: 14, fontWeight: 800, fontFamily: t.fontDisplay }}>Voice Only</span>
          </div>
        )}
        <div style={{ position: 'absolute', bottom: 8, right: 8, padding: '2px 6px', borderRadius: 4, background: 'rgba(0,0,0,0.7)', color: '#fff', fontSize: 10, fontFamily: t.fontMono }}>
          {replay.duration}
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10, fontSize: 12 }}>
        <ArtistAvatar artist={artist} size={22} t={t}/>
        <span style={{ fontWeight: 700 }}>{artist.name}.</span>
        <span style={{ color: t.textDim, fontFamily: t.fontMono }}>{replay.date} · cc</span>
      </div>
      <div style={{ fontSize: 13, marginTop: 4, fontWeight: 600, color: t.text }}>{replay.title}</div>
      <div style={{ fontSize: 11, color: t.textDim, marginTop: 4, fontFamily: t.fontMono, letterSpacing: 0.3 }}>
        재생 {replay.plays} · 좋아요 {replay.likes} · 댓글 {replay.comments}
      </div>
    </div>
  );
}

// ────────── NOTICE ──────────
function TabNotice({ t, theme, artist }) {
  const notices = NOTICES.filter(n => n.artistId === artist.id);
  return (
    <div style={{ border: `1px solid ${t.line}`, borderRadius: 12, overflow: 'hidden' }}>
      {notices.map((n, i) => (
        <div key={n.id} style={{
          padding: '18px 20px', borderBottom: i === notices.length - 1 ? 'none' : `1px solid ${t.line}`,
          display: 'flex', flexDirection: 'column', gap: 4, cursor: 'pointer',
          background: theme === 'dark' ? 'transparent' : 'rgba(255,255,255,0.5)',
        }}>
          <div style={{ fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 8 }}>
            {n.pinned && <span style={{ fontSize: 10, padding: '2px 6px', background: t.hot, color: '#fff', borderRadius: 3, fontWeight: 800 }}>📌</span>}
            <span>{n.title}</span>
          </div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>{n.date}</div>
        </div>
      ))}
    </div>
  );
}

// ────────── SHOP (external placeholder) ──────────
function TabShop({ t, theme, artist }) {
  return (
    <Section t={t} theme={theme}>
      <div style={{ textAlign: 'center', padding: '60px 20px' }}>
        <div style={{ fontSize: 48, marginBottom: 14 }}>🛍</div>
        <div style={{ fontWeight: 800, fontSize: 18, marginBottom: 6 }}>Connectfin Shop</div>
        <div style={{ fontSize: 13, color: t.textDim, marginBottom: 18 }}>
          {artist.name} 공식 상품을 외부 샵에서 확인하세요.
        </div>
        <button style={{
          padding: '12px 28px', borderRadius: 10, border: 'none',
          background: t.gradient, color: '#fff', fontWeight: 800, fontSize: 13, cursor: 'pointer',
        }}>Shop 바로가기 ↗</button>
      </div>
    </Section>
  );
}

// ────────── Shared pieces ──────────
function Section({ t, theme, children, style }) {
  return (
    <div style={{
      border: `1px solid ${t.line}`, borderRadius: 12,
      background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface,
      padding: 18, ...style,
    }}>{children}</div>
  );
}

function Chip({ t, active, children }) {
  return (
    <button style={{
      padding: '6px 14px', borderRadius: 16, border: active ? 'none' : `1px solid ${t.line}`,
      background: active ? (t.name === 'Y2K Pop' ? t.hot : t.hot) : 'transparent',
      color: active ? '#fff' : t.text, fontSize: 12, fontWeight: 700, cursor: 'pointer',
      fontFamily: t.font, display: 'inline-flex', alignItems: 'center', gap: 4,
    }}>{children}</button>
  );
}

function linkBtn(t) {
  return {
    background: 'transparent', border: 'none', color: t.textDim,
    fontSize: 12, cursor: 'pointer', fontFamily: t.fontMono, letterSpacing: 0.3,
  };
}

function moreBtn(t) {
  return {
    width: '100%', marginTop: 14, padding: '10px 0', borderRadius: 20,
    border: `1px solid ${t.line}`, background: 'transparent', color: t.text,
    fontWeight: 600, fontSize: 12, cursor: 'pointer', fontFamily: t.font,
  };
}

function ArtistPostCard({ post, artist, t, inline }) {
  return (
    <div style={{ padding: inline ? 0 : 18, border: inline ? 'none' : `1px solid ${t.line}`, borderRadius: 12, background: inline ? 'transparent' : 'transparent' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <ArtistAvatar artist={artist} size={36} t={t}/>
        <div>
          <div style={{ fontWeight: 800, fontSize: 14 }}>{artist.name}.</div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>{post.time}</div>
        </div>
      </div>
      <div style={{ fontSize: 14, lineHeight: 1.6, whiteSpace: 'pre-line', color: t.text }}>{post.body}</div>
      <div style={{ display: 'flex', gap: 4, marginTop: 10, fontSize: 11, color: t.textDim }}>
        <span style={{ cursor: 'pointer', fontFamily: t.fontMono }}>번역 보기</span>
      </div>
      <div style={{ display: 'flex', gap: 16, marginTop: 12, fontSize: 12, color: t.textDim }}>
        <span>♡ {post.likes}</span>
        <span>💬 {post.comments}</span>
      </div>
    </div>
  );
}

function FanPostFullCard({ post, artist, t, theme }) {
  return (
    <div style={{
      padding: 18, border: `1px solid ${t.line}`, borderRadius: 12,
      background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <div style={{
          width: 32, height: 32, borderRadius: '50%',
          background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontWeight: 800, fontSize: 11,
        }}>{post.author.slice(0, 2)}</div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 13 }}>{post.author} <span style={{ color: t.accent2 }}>✓✓</span></div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>{post.timeAgo}</div>
        </div>
      </div>
      {post.tag && <div style={{ color: t.hot, fontSize: 13, fontWeight: 700, marginBottom: 4 }}>{post.tag}</div>}
      <div style={{ fontSize: 13, lineHeight: 1.55, whiteSpace: 'pre-line', color: t.text }}>{post.body}</div>
      {post.hasGrid && (
        <div style={{ display: 'grid', gridTemplateColumns: `repeat(${post.gridCount}, 1fr)`, gap: 4, marginTop: 12 }}>
          {[...Array(post.gridCount)].map((_, i) => (
            <div key={i} style={{
              aspectRatio: '3/5', borderRadius: 6, overflow: 'hidden',
              background: i % 2 === 0
                ? `linear-gradient(180deg, #FFB5D8, #FF8AC5)`
                : `linear-gradient(180deg, #B5E3D8, #8AD4C3)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: 'rgba(255,255,255,0.9)', fontSize: 9, fontWeight: 700,
              fontFamily: t.fontMono,
            }}>PHOTO</div>
          ))}
        </div>
      )}
      <div style={{ fontSize: 11, color: t.textDim, marginTop: 8, fontFamily: t.fontMono }}>번역 보기</div>
      <div style={{ display: 'flex', gap: 16, marginTop: 10, fontSize: 12, color: t.textDim }}>
        <span>♡ {post.likes}</span>
        {post.comments !== null && <span>💬 {post.comments}</span>}
      </div>
    </div>
  );
}

function FanPostMiniCard({ post, artist, t }) {
  return (
    <div style={{ display: 'flex', gap: 10, padding: 10, borderRadius: 8, border: `1px solid ${t.line}`, cursor: 'pointer' }}>
      <div style={{
        width: 40, height: 40, borderRadius: '50%', flexShrink: 0,
        background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
      }}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 11, fontWeight: 700, marginBottom: 2 }}>{post.author}</div>
        <div style={{ fontSize: 11, color: t.text, lineHeight: 1.4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
          {post.body}
        </div>
        <div style={{ fontSize: 10, color: t.textDim, marginTop: 4 }}>좋아요 {post.likes}</div>
      </div>
    </div>
  );
}

Object.assign(window, {
  DesktopArtistProfile, ArtistCoverBanner,
});

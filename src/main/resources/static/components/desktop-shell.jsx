// Connectfin — Desktop shell: global nav + content column + right sidebar

function DesktopShell({ t, theme, children, activeArtist, onNavGlobal, globalView, onArtistOpen, onNavProfile, profileTab }) {
  return (
    <div style={{
      minHeight: '100vh', width: '100%', background: t.bg, color: t.text,
      fontFamily: t.font, display: 'flex',
    }}>
      <DesktopLeftNav t={t} theme={theme} globalView={globalView} onNavGlobal={onNavGlobal} onArtistOpen={onArtistOpen}/>
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <DesktopTopbar t={t} theme={theme} activeArtist={activeArtist} globalView={globalView} profileTab={profileTab} onNavProfile={onNavProfile}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          {children}
        </div>
      </div>
    </div>
  );
}

// ───────── Left nav ─────────
function DesktopLeftNav({ t, theme, globalView, onNavGlobal, onArtistOpen }) {
  const dark = theme === 'dark';
  return (
    <aside style={{
      width: 240, flexShrink: 0, borderRight: `1px solid ${t.line}`,
      padding: '18px 16px', position: 'sticky', top: 0, height: '100vh',
      overflowY: 'auto', background: dark ? 'rgba(10,11,16,0.5)' : 'rgba(255,255,255,0.5)',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 8px 18px' }}>
        <Infinity8 size={26} color={t.accent} color2={t.accent2} stroke={8}/>
        <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 18, letterSpacing: -0.4 }}>Connectfin</div>
      </div>

      <NavItem t={t} icon="⌂" label="홈" active={globalView === 'home'} onClick={() => onNavGlobal('home')}/>
      <NavItem t={t} icon="✉" label="DM" active={globalView === 'dm'} onClick={() => onNavGlobal('dm')}/>
      <NavItem t={t} icon="⋯" label="더보기" />

      <NavSection t={t} label="내 커뮤니티"/>
      {ARTISTS.slice(0, 6).map(a => (
        <NavArtist key={a.id} artist={a} t={t} onClick={() => onArtistOpen(a.id)}/>
      ))}
      <NavItem t={t} icon="⊕" label="커뮤니티 찾기"/>

      <NavSection t={t} label="서비스 바로가기"/>
      <NavItem t={t} icon="🛍" label="Shop" onClick={() => onNavGlobal('shop')} active={globalView === 'shop'}/>
      <NavItem t={t} icon="🍮" label="Jelly Shop" onClick={() => onNavGlobal('jelly')} active={globalView === 'jelly'}/>

      <div style={{
        marginTop: 20, padding: 12, borderRadius: 10, border: `1px dashed ${t.line}`,
        fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, lineHeight: 1.5, letterSpacing: 0.3,
      }}>
        DEV NOTE<br/>
        좌측 네비 = user.subscribed_communities
        <br/>GET /me/communities
      </div>
    </aside>
  );
}

function NavItem({ icon, label, active, onClick, t }) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 12, width: '100%', textAlign: 'left',
      padding: '9px 10px', borderRadius: 10,
      background: active ? t.chip : 'transparent',
      color: active ? t.accent : t.text,
      border: 'none', cursor: onClick ? 'pointer' : 'default',
      fontSize: 14, fontWeight: active ? 700 : 500, fontFamily: t.font, marginBottom: 2,
    }}>
      <span style={{ fontSize: 16, width: 20, textAlign: 'center' }}>{icon}</span>
      <span>{label}</span>
    </button>
  );
}

function NavSection({ label, t }) {
  return (
    <div style={{
      fontFamily: t.fontMono, fontSize: 10, letterSpacing: 1,
      color: t.textMuted, padding: '18px 10px 8px',
    }}>{label.toUpperCase()}</div>
  );
}

function NavArtist({ artist, t, onClick }) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 10, width: '100%', textAlign: 'left',
      padding: '6px 10px', borderRadius: 10, background: 'transparent',
      color: t.text, border: 'none', cursor: 'pointer',
      fontSize: 13, fontWeight: 600, fontFamily: t.font, marginBottom: 2, position: 'relative',
    }}>
      <ArtistAvatar artist={artist} size={22} t={t}/>
      <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{artist.name}</span>
      {artist.live && (
        <span style={{
          width: 6, height: 6, borderRadius: '50%', background: t.hot,
          boxShadow: `0 0 8px ${t.hot}`, animation: 'connectfin-pulse 1.2s ease-in-out infinite',
        }}/>
      )}
    </button>
  );
}

// ───────── Top bar (profile tabs live here when on artist page) ─────────
function DesktopTopbar({ t, theme, activeArtist, globalView, profileTab, onNavProfile }) {
  const dark = theme === 'dark';
  return (
    <header style={{
      height: 56, flexShrink: 0, display: 'flex', alignItems: 'center',
      padding: '0 24px', borderBottom: `1px solid ${t.line}`,
      position: 'sticky', top: 0, zIndex: 20,
      background: dark ? 'rgba(10,11,16,0.72)' : 'rgba(255,255,255,0.8)',
      backdropFilter: 'blur(20px)',
    }}>
      {globalView === 'artist' && activeArtist ? (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 160 }}>
            <ArtistAvatar artist={activeArtist} size={28} t={t}/>
            <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 16 }}>{activeArtist.name}</div>
            <button style={{
              padding: '4px 10px', borderRadius: 14, border: 'none',
              background: t.chip, color: t.accent, fontSize: 11, fontWeight: 700, cursor: 'pointer',
              fontFamily: t.fontMono, letterSpacing: 0.3,
            }}>+ 가입하기</button>
          </div>
          <nav style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: 24, flex: 1 }}>
            {PROFILE_TABS.map(tab => {
              const active = profileTab === tab.k;
              return (
                <button key={tab.k} onClick={() => onNavProfile(tab.k)} style={{
                  padding: '18px 14px', background: 'transparent', border: 'none',
                  borderBottom: active ? `2px solid ${t.accent}` : '2px solid transparent',
                  color: active ? t.accent : t.text,
                  fontSize: 14, fontWeight: active ? 800 : 600, cursor: 'pointer',
                  fontFamily: t.font,
                }}>
                  {tab.label}{tab.external && <span style={{ fontSize: 10, marginLeft: 4, opacity: 0.6 }}>↗</span>}
                </button>
              );
            })}
          </nav>
        </>
      ) : (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 16 }}>
          <div style={{ fontFamily: t.fontDisplay, fontSize: 17, fontWeight: 800 }}>
            {globalView === 'home' ? '홈' : globalView === 'jelly' ? 'Jelly Shop' : globalView === 'shop' ? 'Shop' : globalView === 'dm' ? 'DM' : globalView === 'live' ? 'LIVE' : ''}
          </div>
        </div>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginLeft: 'auto' }}>
        <span style={{ fontFamily: t.fontMono, fontSize: 11, color: t.textDim }}>142 🍮</span>
        <span style={{ fontSize: 18, cursor: 'pointer' }}>🔔</span>
        <div style={{
          width: 30, height: 30, borderRadius: '50%', background: t.gradient,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontWeight: 800, fontSize: 12, cursor: 'pointer',
        }}>ME</div>
      </div>
    </header>
  );
}

const PROFILE_TABS = [
  { k: 'highlight', label: 'Highlight' },
  { k: 'fan', label: 'Fan' },
  { k: 'artist', label: 'Artist' },
  { k: 'fanletter', label: 'Fan Letter' },
  { k: 'media', label: 'Media' },
  { k: 'live', label: 'LIVE' },
  { k: 'notice', label: 'Notice' },
  { k: 'shop', label: 'Shop', external: true },
];

// ───────── Right sidebar ─────────
function DesktopRightSidebar({ t, theme, artist, onOpenDM, onOpenMembership }) {
  const dark = theme === 'dark';
  return (
    <aside style={{ width: 320, flexShrink: 0, padding: '20px 20px 20px 0', display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* Digital membership */}
      <div style={{
        borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
        background: dark ? '#141624' : t.surface,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <div style={{ width: 20, height: 20, borderRadius: 6, background: t.accent2, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#fff' }}>D</div>
          <div style={{ fontWeight: 800, fontSize: 14 }}>{artist?.name}. Digital membership</div>
        </div>
        <div style={{ fontSize: 12, color: t.textDim, lineHeight: 1.5, marginBottom: 12 }}>
          디지털 멤버십에 가입하고 다양한 혜택을 즐겨보세요.
        </div>
        <button onClick={onOpenMembership} style={{
          width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
          background: t.accent2, color: dark ? '#071014' : '#fff',
          fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
        }}>디지털 멤버십 가입하기</button>
      </div>

      {/* Premium membership */}
      <div style={{
        borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
        background: dark ? '#141624' : t.surface,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <div style={{ width: 20, height: 20, borderRadius: 6, background: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#fff' }}>M</div>
          <div style={{ fontWeight: 800, fontSize: 14 }}>{artist?.name}. Membership</div>
        </div>
        <div style={{ fontSize: 12, color: t.textDim, lineHeight: 1.5, marginBottom: 12 }}>
          지금 멤버십에 가입하고 특별한 혜택을 누려보세요.
        </div>
        <button onClick={onOpenMembership} style={{
          width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
          background: t.accent2, color: dark ? '#071014' : '#fff',
          fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
        }}>멤버십 가입하기</button>
      </div>

      {/* DM widget */}
      <div style={{
        borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
        background: dark ? '#141624' : t.surface,
      }}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 10 }}>Connectfin DM</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <ArtistAvatar artist={artist || ARTISTS[0]} size={32} t={t}/>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 13 }}>{artist?.name || ARTISTS[0].name}.</div>
            <div style={{
              display: 'inline-block', padding: '3px 10px', borderRadius: 12,
              background: t.accent2, color: dark ? '#071014' : '#fff',
              fontSize: 11, fontWeight: 700, marginTop: 2,
            }}>지금 뭐해?</div>
          </div>
        </div>
        <button onClick={onOpenDM} style={{
          width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
          background: t.accent2, color: dark ? '#071014' : '#fff',
          fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
        }}>DM 시작하기</button>
      </div>

      {/* About artist */}
      {artist && (
        <div style={{
          borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
          background: dark ? '#141624' : t.surface,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
            <ArtistAvatar artist={artist} size={36} t={t}/>
            <div>
              <div style={{ fontWeight: 800, fontSize: 15 }}>{artist.name}</div>
              <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono, letterSpacing: 0.3 }}>
                {artist.genre}
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 10, fontSize: 12, color: t.textDim }}>
            <span>▶ YouTube</span>
            <span>✖ X</span>
            <span>◐ IG</span>
          </div>
          <div style={{ fontSize: 12, color: t.textDim, lineHeight: 1.55 }}>
            팬과 아티스트가 가장 가까운 거리에서 연결되는 디지털 경험.
            {artist.stage}의 무대 뒤 순간들, 새로운 이야기들을 Connectfin에서 만나보세요.
          </div>
        </div>
      )}
    </aside>
  );
}

Object.assign(window, {
  DesktopShell, DesktopRightSidebar, PROFILE_TABS,
});

// Connectfin — Desktop DM screen (3-column layout)
// Layout: Thread list (left) · Active conversation (right)

function DesktopDMScreen({ t, theme, onExit }) {
  const [rooms, setRooms] = React.useState(DM_THREADS);
  const [activeId, setActiveId] = React.useState(DM_THREADS[0]?.id || 1);
  const [filter, setFilter] = React.useState('all'); // all | unread | subscribed

  React.useEffect(() => {
    if (!window.ConnectfinAPI.getToken()) return;
    window.ConnectfinAPI.api('/api/v1/dm/rooms')
      .then(data => {
        if (data && data.length > 0) {
          const mapped = data.map(room => ({
            id: room.id,
            artistId: room.artistId,
            name: ARTISTS.find(a => a.id === room.artistId)?.name || `Artist ${room.artistId}`,
            last: '',
            unread: 0,
            subscribed: true,
          }));
          setRooms(mapped);
          setActiveId(mapped[0].id);
        }
      })
      .catch(() => { /* mock 유지 */ });
  }, []);

  const activeThread = rooms.find(x => x.id === activeId) || rooms[0];
  const activeArtist = ARTISTS.find(a => a.id === activeThread?.artistId);

  const filtered = rooms.filter(th => {
    if (filter === 'unread') return (th.unread || 0) > 0;
    if (filter === 'subscribed') return th.subscribed;
    return true;
  });

  const dark = theme === 'dark';
  const sidebarBg = dark ? '#0E1019' : '#FAFAFA';
  const threadBg = dark ? 'rgba(20,22,36,0.55)' : '#fff';

  return (
    <div style={{
      display: 'grid', gridTemplateColumns: '340px 1fr',
      height: 'calc(100vh - 56px)', color: t.text,
    }}>
      {/* ── Left: Thread list ── */}
      <div style={{
        borderRight: `1px solid ${t.line}`, background: sidebarBg,
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ padding: '20px 20px 14px', borderBottom: `1px solid ${t.line}` }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
            <div>
              <div style={{ fontFamily: t.fontMono, fontSize: 10, letterSpacing: 1.2, color: t.textDim }}>DIRECT MESSAGE</div>
              <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, letterSpacing: -0.3 }}>대화</div>
            </div>
            <button title="새 메시지" style={{
              width: 34, height: 34, borderRadius: '50%', border: `1px solid ${t.line}`,
              background: 'transparent', color: t.text, cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16,
            }}>✎</button>
          </div>

          {/* Search */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '8px 14px',
            borderRadius: 20, background: dark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)',
            border: `1px solid ${t.line}`,
          }}>
            <span style={{ color: t.textDim, fontSize: 13 }}>⌕</span>
            <input placeholder="아티스트 검색" style={{
              flex: 1, border: 'none', background: 'transparent', color: t.text,
              fontSize: 13, outline: 'none',
            }}/>
          </div>

          {/* Filters */}
          <div style={{ display: 'flex', gap: 6, marginTop: 12 }}>
            {[['all','전체'], ['unread','안 읽음'], ['subscribed','구독중']].map(([k, l]) => (
              <button key={k} onClick={() => setFilter(k)} style={{
                padding: '5px 12px', borderRadius: 14, border: 'none', cursor: 'pointer',
                background: filter === k ? t.chip : 'transparent',
                color: filter === k ? t.accent : t.textDim,
                fontWeight: 700, fontSize: 11, fontFamily: t.fontMono, letterSpacing: 0.3,
              }}>{l}</button>
            ))}
          </div>
        </div>

        {/* Thread list */}
        <div style={{ flex: 1, overflowY: 'auto' }}>
          {filtered.length === 0 && (
            <div style={{ padding: '40px 20px', textAlign: 'center', color: t.textDim, fontSize: 13 }}>
              필터에 해당하는 대화가 없어요
            </div>
          )}
          {filtered.map(th => {
            const a = ARTISTS.find(x => x.id === th.artistId);
            const active = th.id === activeId;
            return (
              <div key={th.id} onClick={() => setActiveId(th.id)} style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '14px 18px', cursor: 'pointer',
                background: active ? (dark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)') : 'transparent',
                borderLeft: active ? `3px solid ${t.accent}` : '3px solid transparent',
                borderBottom: `1px solid ${t.line}`,
                transition: 'background 140ms',
              }}
                onMouseEnter={e => { if (!active) e.currentTarget.style.background = dark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)'; }}
                onMouseLeave={e => { if (!active) e.currentTarget.style.background = 'transparent'; }}
              >
                <div style={{ position: 'relative', flexShrink: 0 }}>
                  <div style={{
                    width: 46, height: 46, borderRadius: '50%',
                    background: `linear-gradient(135deg, ${a?.color1 || '#888'}, ${a?.color2 || '#444'})`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color: '#fff', fontWeight: 800, fontSize: 14,
                  }}>{a?.name?.slice(0, 2) || '??'}</div>
                  {th.online && (
                    <div style={{
                      position: 'absolute', bottom: -1, right: -1,
                      width: 12, height: 12, borderRadius: '50%',
                      background: '#22C55E', border: `2.5px solid ${sidebarBg}`,
                    }}/>
                  )}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 8 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4, minWidth: 0 }}>
                      <span style={{ fontWeight: 700, fontSize: 14, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {th.name}
                      </span>
                      {th.subscribed && <span style={{ fontSize: 10, color: t.accent2, flexShrink: 0 }}>✓</span>}
                    </div>
                    <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono, flexShrink: 0 }}>
                      {th.time}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 3 }}>
                    <div style={{
                      flex: 1, fontSize: 12, color: (th.unread || 0) > 0 ? t.text : t.textDim,
                      overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                      fontWeight: (th.unread || 0) > 0 ? 600 : 400,
                    }}>
                      {th.last}
                    </div>
                    {(th.unread || 0) > 0 && (
                      <div style={{
                        minWidth: 18, height: 18, borderRadius: 9,
                        background: t.hot, color: '#fff',
                        fontSize: 10, fontWeight: 800, padding: '0 5px',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                      }}>{th.unread}</div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div style={{
          padding: '12px 18px', borderTop: `1px solid ${t.line}`,
          fontSize: 10, color: t.textDim, fontFamily: t.fontMono, letterSpacing: 0.3,
        }}>
          GET /dm/threads?cursor= · {rooms.length} threads cached
        </div>
      </div>

      {/* ── Right: Active conversation ── */}
      <DMConversationPanel
        key={activeThread.id}
        thread={activeThread} artist={activeArtist}
        t={t} theme={theme}
      />
    </div>
  );
}

function DMConversationPanel({ thread, artist, t, theme }) {
  const dark = theme === 'dark';
  const [messages, setMessages] = React.useState(() => INITIAL_DM_MESSAGES(thread, artist));
  const [input, setInput] = React.useState('');
  const [remaining, setRemaining] = React.useState(2);
  const [showInfo, setShowInfo] = React.useState(false);
  const nextId = React.useRef(100);
  const listRef = React.useRef(null);

  React.useEffect(() => {
    if (!window.ConnectfinAPI.getToken() || !thread?.id) return;
    window.ConnectfinAPI.api(`/api/v1/dm/rooms/${thread.id}/messages`)
      .then(data => {
        if (data.content && data.content.length > 0) {
          setMessages(data.content.map(msg => ({
            from: msg.senderType === 'USER' ? 'me' : (artist?.name || 'Artist'),
            m: msg.content,
            time: window.ConnectfinAPI.formatTime(msg.sentAt),
            mine: msg.senderType === 'USER',
          })));
        }
      })
      .catch(() => { /* mock 유지 */ });
  }, [thread?.id]);

  React.useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [messages]);

  const send = () => {
    if (!input.trim() || remaining <= 0) return;
    setMessages(m => [...m, { id: nextId.current++, from: 'me', m: input, time: '방금' }]);
    setInput('');
    setRemaining(r => r - 1);
  };

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: showInfo ? '1fr 320px' : '1fr',
      background: dark ? '#080912' : '#F5F5F7',
      overflow: 'hidden',
    }}>
      <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* Header */}
        <div style={{
          padding: '14px 24px', borderBottom: `1px solid ${t.line}`,
          display: 'flex', alignItems: 'center', gap: 14,
          background: dark ? 'rgba(14,16,25,0.7)' : '#fff',
          backdropFilter: 'blur(20px)',
        }}>
          <div style={{ position: 'relative' }}>
            <div style={{
              width: 44, height: 44, borderRadius: '50%',
              background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', fontWeight: 800, fontSize: 13,
            }}>{artist.name.slice(0, 2)}</div>
            {thread.online && (
              <div style={{
                position: 'absolute', bottom: -1, right: -1,
                width: 12, height: 12, borderRadius: '50%',
                background: '#22C55E', border: `2.5px solid ${dark ? '#0E1019' : '#fff'}`,
              }}/>
            )}
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontWeight: 800, fontSize: 17, letterSpacing: -0.2 }}>{artist.name}</span>
              <span style={{
                fontSize: 9, fontWeight: 800, padding: '3px 7px', borderRadius: 4,
                background: t.gradient, color: '#fff', letterSpacing: 0.5,
              }}>ARTIST ✓</span>
            </div>
            <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono, marginTop: 2 }}>
              {thread.online ? '● 온라인' : '마지막 접속 3시간 전'} · @{artist.name.toLowerCase()}
            </div>
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <HeaderIconBtn t={t} title="검색">⌕</HeaderIconBtn>
            <HeaderIconBtn t={t} title="음소거">🔕</HeaderIconBtn>
            <HeaderIconBtn t={t} title="상세 정보" active={showInfo} onClick={() => setShowInfo(v => !v)}>ⓘ</HeaderIconBtn>
          </div>
        </div>

        {/* Messages */}
        <div ref={listRef} style={{
          flex: 1, overflowY: 'auto',
          padding: '24px 32px',
          display: 'flex', flexDirection: 'column', gap: 10,
        }}>
          <div style={{
            alignSelf: 'center', padding: '6px 14px', borderRadius: 14,
            background: dark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)',
            fontSize: 11, color: t.textDim, fontFamily: t.fontMono, letterSpacing: 0.3,
            marginBottom: 8,
          }}>🔒 1:1 암호화된 채널 · {thread.time}</div>

          {messages.map((msg, i) => {
            const isMe = msg.from === 'me';
            const prev = messages[i - 1];
            const grouped = prev && prev.from === msg.from;
            return (
              <div key={msg.id} style={{
                display: 'flex', flexDirection: isMe ? 'row-reverse' : 'row',
                alignItems: 'flex-end', gap: 10, marginTop: grouped ? 0 : 8,
              }}>
                {!isMe && (
                  <div style={{
                    width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
                    background: grouped ? 'transparent' : `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color: '#fff', fontWeight: 800, fontSize: 10,
                  }}>{!grouped && artist.name.slice(0, 2)}</div>
                )}
                <div style={{
                  maxWidth: '58%',
                  display: 'flex', flexDirection: 'column',
                  alignItems: isMe ? 'flex-end' : 'flex-start',
                }}>
                  {!grouped && !isMe && (
                    <div style={{ fontSize: 11, fontWeight: 700, color: t.textDim, marginBottom: 4, marginLeft: 2 }}>
                      {artist.name}
                    </div>
                  )}
                  <div style={{
                    padding: '11px 16px',
                    borderRadius: isMe ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                    background: isMe
                      ? t.gradient
                      : (dark ? '#1A1D2A' : '#fff'),
                    color: isMe ? '#fff' : t.text,
                    fontSize: 14, lineHeight: 1.5,
                    border: !isMe && !dark ? `1px solid ${t.line}` : 'none',
                    boxShadow: !isMe && dark ? 'inset 0 0 0 1px rgba(255,255,255,0.06)' : 'none',
                    wordBreak: 'break-word',
                  }}>
                    {msg.m}
                  </div>
                  <span style={{
                    fontSize: 10, color: t.textMuted, fontFamily: t.fontMono,
                    marginTop: 3, padding: '0 4px',
                  }}>
                    {msg.time}{isMe && ' · 읽음'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Rate-limit hint */}
        <div style={{
          padding: '8px 24px',
          background: remaining <= 1 ? (dark ? 'rgba(255,100,100,0.08)' : 'rgba(255,80,80,0.05)') : 'transparent',
          borderTop: `1px solid ${t.line}`,
          fontSize: 11, color: t.textDim, fontFamily: t.fontMono,
          display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <span style={{ color: remaining <= 1 ? t.hot : t.textDim }}>ⓘ</span>
          <span>아티스트 답장 전 최대 3개까지 · 남은 횟수 <b style={{ color: remaining > 0 ? t.accent : t.hot }}>{remaining}</b>개</span>
          <span style={{ marginLeft: 'auto', letterSpacing: 0.3 }}>
            POST /dm/threads/{thread.id}/messages
          </span>
        </div>

        {/* Composer */}
        <div style={{
          padding: '14px 24px 20px',
          background: dark ? 'rgba(14,16,25,0.7)' : '#fff',
          borderTop: `1px solid ${t.line}`,
        }}>
          <div style={{
            display: 'flex', gap: 10, alignItems: 'flex-end',
            padding: '8px 14px', borderRadius: 18,
            border: `1px solid ${t.line}`,
            background: dark ? 'rgba(255,255,255,0.04)' : '#F7F7FA',
          }}>
            <button style={composerBtn(t)} title="이모지">☺</button>
            <button style={composerBtn(t)} title="이미지">🖼</button>
            <button style={composerBtn(t)} title="스티커">🏷</button>
            <textarea
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
              }}
              disabled={remaining <= 0}
              placeholder={remaining <= 0 ? '메시지 제한에 도달했어요' : `${artist.name}에게 메시지… (Enter로 전송, Shift+Enter 줄바꿈)`}
              rows={1}
              style={{
                flex: 1, border: 'none', background: 'transparent', color: t.text,
                fontSize: 14, outline: 'none', resize: 'none',
                padding: '8px 4px', minHeight: 22, maxHeight: 120,
                fontFamily: t.font,
              }}
            />
            <button onClick={send} disabled={remaining <= 0 || !input.trim()} style={{
              padding: '8px 18px', borderRadius: 12, border: 'none',
              background: (remaining > 0 && input.trim()) ? t.gradient : (dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)'),
              color: (remaining > 0 && input.trim()) ? '#fff' : t.textDim,
              fontWeight: 800, fontSize: 13, cursor: (remaining > 0 && input.trim()) ? 'pointer' : 'not-allowed',
              fontFamily: t.fontMono, letterSpacing: 0.5,
            }}>SEND</button>
          </div>
        </div>
      </div>

      {/* Info sidebar */}
      {showInfo && <DMInfoPanel thread={thread} artist={artist} t={t} theme={theme}/>}
    </div>
  );
}

function DMInfoPanel({ thread, artist, t, theme }) {
  const dark = theme === 'dark';
  return (
    <div style={{
      borderLeft: `1px solid ${t.line}`,
      background: dark ? '#0E1019' : '#FAFAFA',
      overflowY: 'auto', padding: 24,
    }}>
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <div style={{
          width: 96, height: 96, borderRadius: '50%',
          background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontWeight: 800, fontSize: 28, margin: '0 auto 12px',
        }}>{artist.name.slice(0, 2)}</div>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, letterSpacing: -0.3 }}>{artist.name}</div>
        <div style={{ fontSize: 12, color: t.textDim, fontFamily: t.fontMono, marginTop: 2 }}>{artist.genre}</div>
      </div>

      <div style={{
        padding: 14, borderRadius: 12, marginBottom: 14,
        border: `1px solid ${t.line}`,
        background: dark ? 'rgba(20,22,36,0.55)' : '#fff',
      }}>
        <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: 1, color: t.textDim, fontFamily: t.fontMono, marginBottom: 8 }}>
          SUBSCRIPTION
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13 }}>
          <span>DM Pass</span>
          <span style={{ fontWeight: 700, color: t.accent }}>15일 남음</span>
        </div>
        <div style={{
          marginTop: 10, height: 5, borderRadius: 3,
          background: dark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)',
          overflow: 'hidden',
        }}>
          <div style={{ width: '50%', height: '100%', background: t.gradient }}/>
        </div>
        <button style={{
          width: '100%', marginTop: 12, padding: '9px 0', borderRadius: 8, border: 'none',
          background: t.chip, color: t.accent, fontWeight: 800, fontSize: 12, cursor: 'pointer',
          fontFamily: t.fontMono, letterSpacing: 0.5,
        }}>연장하기 · ₩9,900/월</button>
      </div>

      <InfoRow t={t} label="DM 시작일">2026.01.12</InfoRow>
      <InfoRow t={t} label="총 주고받은 메시지">42</InfoRow>
      <InfoRow t={t} label="이번 달 보낸 메시지">7 / 30</InfoRow>
      <InfoRow t={t} label="알림">켜짐</InfoRow>
      <InfoRow t={t} label="번역">자동 (ko ↔ jp)</InfoRow>

      <div style={{ marginTop: 24 }}>
        <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: 1, color: t.textDim, fontFamily: t.fontMono, marginBottom: 10 }}>
          공유된 미디어
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6 }}>
          {[0,1,2,3,4,5].map(i => (
            <div key={i} style={{
              aspectRatio: '1', borderRadius: 6,
              background: `linear-gradient(${i * 60}deg, ${artist.color1}, ${artist.color2})`,
              opacity: 0.7,
            }}/>
          ))}
        </div>
      </div>

      <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 6 }}>
        <button style={infoActionBtn(t, theme)}>🔕 알림 끄기</button>
        <button style={infoActionBtn(t, theme)}>🚫 신고 & 차단</button>
      </div>
    </div>
  );
}

function InfoRow({ t, label, children }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      padding: '10px 0', fontSize: 13, borderBottom: `1px solid ${t.line}`,
    }}>
      <span style={{ color: t.textDim }}>{label}</span>
      <span style={{ color: t.text, fontWeight: 600 }}>{children}</span>
    </div>
  );
}

function HeaderIconBtn({ t, children, onClick, active, title }) {
  const [h, setH] = React.useState(false);
  return (
    <button onClick={onClick} title={title}
      onMouseEnter={() => setH(true)} onMouseLeave={() => setH(false)}
      style={{
        width: 38, height: 38, borderRadius: 10, border: 'none', cursor: 'pointer',
        background: active ? t.chip : (h ? 'rgba(128,128,128,0.1)' : 'transparent'),
        color: active ? t.accent : t.text,
        fontSize: 16,
      }}>{children}</button>
  );
}

function composerBtn(t) {
  return {
    width: 34, height: 34, borderRadius: 8, border: 'none', cursor: 'pointer',
    background: 'transparent', color: t.textDim,
    fontSize: 16, display: 'flex', alignItems: 'center', justifyContent: 'center',
  };
}

function infoActionBtn(t, theme) {
  return {
    padding: '10px 14px', borderRadius: 8, border: `1px solid ${t.line}`,
    background: 'transparent', color: t.text, cursor: 'pointer',
    fontSize: 12, fontWeight: 600, textAlign: 'left',
    fontFamily: t.font,
  };
}

function INITIAL_DM_MESSAGES(thread, artist) {
  const base = {
    1: [ // YUNA / LUMEN8
      { id: 1, from: 'artist', m: '안녕! 오늘 하루도 수고 많았지 🫶', time: '어제 20:14' },
      { id: 2, from: 'artist', m: '요즘 어떻게 지내?', time: '어제 20:14' },
      { id: 3, from: 'me', m: '언니 저 오늘 시험 끝났어요!!! 진짜 살 거 같아요 😭', time: '어제 20:16' },
      { id: 4, from: 'artist', m: '오~ 수고했어!! 이제 좀 쉬어', time: '어제 20:17' },
      { id: 5, from: 'artist', m: '근데 우리 내일 라이브 있는데 올 수 있어?', time: '어제 20:17' },
      { id: 6, from: 'me', m: '당연하죠!! 매일 챙겨봐요 🙌', time: '어제 20:18' },
      { id: 7, from: 'artist', m: '오늘도 고마워 ⭐️', time: '방금' },
    ],
    2: [ // hanabi*
      { id: 1, from: 'artist', m: 'やっほー！', time: '10분 전' },
      { id: 2, from: 'artist', m: '새 앨범 프리오더 들어봤어?!', time: '3분 전' },
      { id: 3, from: 'me', m: '하나비 짱!! 당연히 들었어요 🎆', time: '방금' },
    ],
    3: [ // Kagerō
      { id: 1, from: 'artist', m: 'Mirage、\n聴いてくれてありがとう。', time: '1시간 전' },
      { id: 2, from: 'me', m: '이번 앨범 최고예요 진짜', time: '1시간 전' },
    ],
    4: [ // NOIR7 KAI
      { id: 1, from: 'artist', m: '앙코르 콘서트 곧이야 !! 준비 중이야', time: '2시간 전' },
      { id: 2, from: 'artist', m: '이번엔 특별한 거 준비했어 😏', time: '2시간 전' },
      { id: 3, from: 'me', m: '세븐츠 기대 만발이에요 💜', time: '1시간 전' },
      { id: 4, from: 'artist', m: '현장에서 보자!', time: '30분 전' },
    ],
  };
  return base[thread.id] || [
    { id: 1, from: 'artist', m: `안녕 ${thread.name}입니다! 메시지 고마워요 🙌`, time: '방금' },
  ];
}

Object.assign(window, { DesktopDMScreen });

// Live streaming + realtime chat, DM list + thread, Community feed

function LiveScreen({ t, theme, artistId, speed, liveOn, onBack, onNav }) {
  const a = ARTISTS.find(x => x.id === artistId) || ARTISTS.find(x => x.live) || ARTISTS[0];
  const [messages, setMessages] = React.useState(() =>
    LIVE_SEEDS.slice(0, 6).map((s, i) => ({ ...s, id: i, t: s.t || 'fan' }))
  );
  const [viewers, setViewers] = React.useState(a.viewers || 58_000);
  const [hearts, setHearts] = React.useState([]);
  const [input, setInput] = React.useState('');
  const [stompConnected, setStompConnected] = React.useState(false);
  const listRef = React.useRef(null);
  const nextId = React.useRef(100);

  React.useEffect(() => {
    if (!liveOn || stompConnected) return; // STOMP 연결 시 mock 비활성화
    const id = setInterval(() => {
      const seed = LIVE_SEEDS[Math.floor(Math.random() * LIVE_SEEDS.length)];
      setMessages(m => [...m.slice(-40), { ...seed, id: nextId.current++, t: seed.t || 'fan' }]);
      setViewers(v => Math.max(1000, v + Math.round((Math.random() - 0.4) * 300 * speed)));
      if (Math.random() < 0.5) {
        setHearts(h => [...h.slice(-8), { id: nextId.current++, x: 20 + Math.random() * 60, d: 1500 + Math.random() * 1500 }]);
      }
    }, 700 / speed);
    return () => clearInterval(id);
  }, [speed, liveOn, stompConnected]);

  // STOMP 라이브 채팅 연결
  React.useEffect(() => {
    if (!liveOn || !window.ConnectfinAPI.getToken()) return;

    window.ConnectfinAPI.connectStomp(
      () => {
        setStompConnected(true);
        window.ConnectfinAPI.subscribeLive(a.id, (batch) => {
          const mapped = batch.map(msg => ({
            id: msg.id || nextId.current++,
            u: msg.senderUserId === 'me' ? 'me' : `user_${msg.senderUserId}`,
            m: msg.message,
            t: 'fan',
          }));
          setMessages(prev => [...prev.slice(-40), ...mapped]);
        });
      },
      (err) => {
        console.warn('STOMP connection failed, using mock:', err);
      }
    );

    return () => {
      window.ConnectfinAPI.unsubscribe(`live:${a.id}`);
    };
  }, [liveOn, a.id]);

  React.useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [messages]);

  const send = () => {
    if (!input.trim()) return;
    if (stompConnected) {
      // STOMP로 전송 — 서버가 브로드캐스트하면 subscribeLive에서 수신
      window.ConnectfinAPI.sendLiveChat(a.id, input);
    }
    // 로컬에도 즉시 추가 (optimistic)
    setMessages(m => [...m, { id: nextId.current++, u: 'me', m: input, t: 'me' }]);
    setInput('');
  };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#000', position: 'relative' }}>
      {/* Video area */}
      <div style={{ position: 'relative', height: '48%', overflow: 'hidden' }}>
        <div style={{
          position: 'absolute', inset: 0,
          background: `repeating-linear-gradient(30deg, ${a.color1} 0 24px, ${a.color2} 24px 48px)`,
          animation: liveOn ? 'connectfin-shimmer 8s linear infinite' : 'none',
        }}/>
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(0,0,0,0.55) 0%, rgba(0,0,0,0) 30%, rgba(0,0,0,0) 60%, rgba(0,0,0,0.6) 100%)' }}/>
        <div style={{
          position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
          fontFamily: t.fontMono, color: 'rgba(255,255,255,0.7)', fontSize: 11, letterSpacing: 1,
        }}>LIVE VIDEO STREAM · {a.name}</div>

        {/* Back */}
        <button onClick={onBack} style={{
          position: 'absolute', top: 62, left: 14, zIndex: 3,
          width: 34, height: 34, borderRadius: '50%',
          background: 'rgba(0,0,0,0.55)', color: '#fff', border: 'none', fontSize: 16, cursor: 'pointer',
          backdropFilter: 'blur(10px)',
        }}>←</button>

        {/* Live badge + viewers */}
        <div style={{ position: 'absolute', top: 62, right: 14, display: 'flex', gap: 6, zIndex: 3 }}>
          <div style={{
            padding: '5px 10px', borderRadius: 6, background: t.liveGradient, color: '#fff',
            fontSize: 11, fontWeight: 800, letterSpacing: 0.5, display: 'flex', alignItems: 'center', gap: 5,
          }}>
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#fff', animation: 'connectfin-pulse 1.2s ease-in-out infinite' }}/>
            LIVE
          </div>
          <div style={{
            padding: '5px 10px', borderRadius: 6, background: 'rgba(0,0,0,0.6)',
            color: '#fff', fontSize: 11, fontFamily: t.fontMono, backdropFilter: 'blur(10px)',
          }}>👁 {viewers.toLocaleString()}</div>
        </div>

        {/* Title */}
        <div style={{ position: 'absolute', bottom: 20, left: 16, right: 16, color: '#fff', zIndex: 3 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
            <ArtistAvatar artist={a} size={40} t={t}/>
            <div>
              <div style={{ fontWeight: 800, fontSize: 15 }}>{a.stage}</div>
              <div style={{ fontSize: 11, opacity: 0.8, fontFamily: t.fontMono }}>@{a.name.toLowerCase()} · {a.genre}</div>
            </div>
            <button style={{
              marginLeft: 'auto', padding: '6px 14px', borderRadius: 20, border: '1px solid rgba(255,255,255,0.4)',
              background: 'rgba(255,255,255,0.12)', color: '#fff', fontSize: 12, fontWeight: 700, cursor: 'pointer',
              backdropFilter: 'blur(10px)',
            }}>+ Follow</button>
          </div>
          <div style={{ fontFamily: t.fontDisplay, fontSize: 18, fontWeight: 800, lineHeight: 1.2 }}>
            {a.name === 'hanabi*' ? '🎙️ 늦밤 잡담 · 새 커버곡 첫 공개' : '🌙 퇴근길 LIVE · 오늘도 고생했어'}
          </div>
        </div>

        {/* Floating hearts */}
        <div style={{ position: 'absolute', bottom: 0, right: 0, width: '40%', height: '100%', pointerEvents: 'none' }}>
          {hearts.map(h => (
            <div key={h.id} style={{
              position: 'absolute', bottom: 0, right: `${h.x}%`,
              fontSize: 20, animation: `connectfin-float ${h.d}ms ease-out forwards`,
            }}>💜</div>
          ))}
        </div>
      </div>

      {/* Chat */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: t.bg, position: 'relative' }}>
        <div style={{
          padding: '10px 16px 6px', borderBottom: `1px solid ${t.line}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <span style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 14 }}>Live Chat</span>
          <span style={{ fontFamily: t.fontMono, fontSize: 10, color: t.textDim }}>
            batch · 200~500ms · ws/sub/live/{a.id}
          </span>
        </div>
        <div ref={listRef} style={{
          flex: 1, overflowY: 'auto', padding: '10px 14px',
          display: 'flex', flexDirection: 'column', gap: 6,
        }}>
          {messages.map(msg => {
            const isArtist = msg.t === 'artist';
            const isMe = msg.t === 'me';
            return (
              <div key={msg.id} style={{
                display: 'flex', gap: 8, alignItems: 'flex-start',
                fontSize: 13, lineHeight: 1.4,
                animation: 'connectfin-chatin 240ms ease-out',
              }}>
                <span style={{
                  fontWeight: isArtist ? 800 : 700, fontSize: 12,
                  color: isArtist ? t.accent : isMe ? t.accent2 : t.textDim,
                  flexShrink: 0, maxWidth: 100, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                }}>
                  {isArtist && '⭐ '}{msg.u}
                </span>
                <span style={{ color: t.text, wordBreak: 'break-word', flex: 1 }}>{msg.m}</span>
              </div>
            );
          })}
        </div>
        <div style={{
          padding: '8px 12px', borderTop: `1px solid ${t.line}`,
          display: 'flex', gap: 8, alignItems: 'center',
        }}>
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && send()}
            placeholder="메시지 보내기 · 금칙어 필터링됨"
            style={{
              flex: 1, padding: '10px 14px', borderRadius: 20,
              border: `1px solid ${t.line}`, background: t.surface, color: t.text,
              fontSize: 13, fontFamily: t.font,
            }}
          />
          <button onClick={send} style={{
            width: 38, height: 38, borderRadius: '50%', border: 'none',
            background: t.gradient, color: '#fff', fontSize: 16, cursor: 'pointer',
          }}>→</button>
        </div>
      </div>
    </div>
  );
}

function DMListScreen({ t, onOpenDM }) {
  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingTop: 60, paddingBottom: 96 }}>
      <div style={{ padding: '0 20px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <div style={{ fontFamily: t.fontDisplay, fontSize: 28, fontWeight: 800, letterSpacing: -0.8 }}>Messages</div>
          <div style={{ fontSize: 12, color: t.textDim, fontFamily: t.fontMono, marginTop: 2 }}>
            4 rooms · 3 active subs
          </div>
        </div>
        <div style={{
          padding: '4px 10px', borderRadius: 20, background: t.chip,
          fontSize: 11, fontWeight: 700, color: t.accent, fontFamily: t.fontMono,
        }}>+ 구독권</div>
      </div>

      <div style={{ padding: '0 12px' }}>
        {DM_THREADS.map(th => {
          const a = ARTISTS.find(x => x.id === th.artistId);
          return (
            <button key={th.id} onClick={() => onOpenDM(th.id)} style={{
              width: '100%', display: 'flex', gap: 12, alignItems: 'center',
              padding: '12px 12px', background: 'transparent', border: 'none',
              cursor: 'pointer', textAlign: 'left', borderRadius: 14,
              opacity: th.subscribed ? 1 : 0.6,
            }}>
              <div style={{ position: 'relative' }}>
                <ArtistAvatar artist={a} size={52} t={t}/>
                {th.online && <div style={{
                  position: 'absolute', bottom: 2, right: 2, width: 12, height: 12,
                  borderRadius: '50%', background: t.ok, border: `2px solid ${t.bg}`,
                }}/>}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                    <span style={{ fontWeight: 800, fontSize: 15, color: t.text }}>{th.name}</span>
                    <span style={{ fontSize: 10, fontWeight: 700, padding: '2px 6px', borderRadius: 4, background: t.chip, color: t.accent, fontFamily: t.fontMono }}>
                      {th.group}
                    </span>
                  </div>
                  <span style={{ fontSize: 10, color: t.textMuted, fontFamily: t.fontMono }}>{th.time}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 2 }}>
                  <span style={{
                    fontSize: 13, color: th.subscribed ? t.textDim : t.textMuted,
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 200,
                    fontStyle: !th.subscribed ? 'italic' : 'normal',
                  }}>
                    {th.last}
                  </span>
                  {th.unread > 0 && (
                    <div style={{
                      minWidth: 20, height: 20, padding: '0 6px', borderRadius: 10,
                      background: t.hot, color: '#fff', fontSize: 11, fontWeight: 800,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>{th.unread}</div>
                  )}
                </div>
              </div>
            </button>
          );
        })}
      </div>

      <div style={{
        margin: '20px 20px 0', padding: 16, borderRadius: 18,
        background: t.gradient, color: '#fff',
      }}>
        <div style={{ fontSize: 11, fontFamily: t.fontMono, letterSpacing: 0.5, opacity: 0.9 }}>DM SUBSCRIPTION</div>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 18, fontWeight: 800, marginTop: 2 }}>
          NOIR7의 DM을 잠금해제하세요
        </div>
        <div style={{ fontSize: 12, marginTop: 4, opacity: 0.9 }}>월 15 Jelly · 답장 전 최대 3개 메시지</div>
      </div>
    </div>
  );
}

function DMThreadScreen({ t, theme, threadId, onBack }) {
  const th = DM_THREADS.find(x => x.id === threadId) || DM_THREADS[0];
  const a = ARTISTS.find(x => x.id === th.artistId);
  const [messages, setMessages] = React.useState([
    { id: 1, from: 'artist', m: '안녕! 오늘 하루도 수고 많았지 🫶', time: '20:14' },
    { id: 2, from: 'artist', m: '[[name]]아 요즘 어떻게 지내?', time: '20:14' },
    { id: 3, from: 'me', m: '언니 저 오늘 시험 끝났어요!!! 진짜 살 거 같아요 😭', time: '20:16' },
    { id: 4, from: 'artist', m: '오~ 수고했어!! 이제 좀 쉬어', time: '20:17' },
    { id: 5, from: 'me', m: '라이브 매일 챙겨볼게요 🙌', time: '20:17' },
    { id: 6, from: 'artist', m: '오늘도 고마워 ⭐️', time: '방금' },
  ]);
  const [input, setInput] = React.useState('');
  const [remaining, setRemaining] = React.useState(2); // 3 - already sent 1
  const nextId = React.useRef(100);
  const listRef = React.useRef(null);

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
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: t.bg }}>
      {/* Header */}
      <div style={{
        paddingTop: 54, padding: '54px 14px 12px', display: 'flex', alignItems: 'center', gap: 10,
        borderBottom: `1px solid ${t.line}`, background: t.bg,
      }}>
        <button onClick={onBack} style={{
          background: 'transparent', border: 'none', color: t.text, fontSize: 20, cursor: 'pointer', padding: 4,
        }}>←</button>
        <ArtistAvatar artist={a} size={36} t={t}/>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontWeight: 800, fontSize: 15 }}>{th.name}</span>
            <span style={{ fontSize: 9, fontWeight: 800, padding: '2px 5px', borderRadius: 3, background: t.gradient, color: '#fff' }}>ARTIST ✓</span>
          </div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>
            {th.online ? '● 온라인' : '오프라인'} · 구독 {remaining === 3 ? '~15일' : '만료 12일'}
          </div>
        </div>
        <div style={{ color: t.textDim, fontSize: 18 }}>⋯</div>
      </div>

      {/* Messages */}
      <div ref={listRef} style={{
        flex: 1, overflowY: 'auto', padding: '16px 14px',
        display: 'flex', flexDirection: 'column', gap: 8,
      }}>
        <div style={{
          alignSelf: 'center', padding: '4px 10px', borderRadius: 10,
          background: t.surface2, fontSize: 10, color: t.textMuted, fontFamily: t.fontMono,
        }}>DM · 암호화된 1:1 채널</div>
        {messages.map(msg => {
          const isMe = msg.from === 'me';
          return (
            <div key={msg.id} style={{
              display: 'flex', flexDirection: isMe ? 'row-reverse' : 'row',
              alignItems: 'flex-end', gap: 6,
            }}>
              {!isMe && <ArtistAvatar artist={a} size={26} t={t}/>}
              <div style={{
                maxWidth: '72%', padding: '10px 13px',
                borderRadius: isMe ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                background: isMe ? t.gradient : t.surface,
                color: isMe ? '#fff' : t.text,
                fontSize: 13.5, lineHeight: 1.45,
                border: !isMe ? `1px solid ${t.line}` : 'none',
              }}>
                {msg.m}
              </div>
              <span style={{ fontSize: 9, color: t.textMuted, fontFamily: t.fontMono, marginBottom: 2 }}>{msg.time}</span>
            </div>
          );
        })}
      </div>

      {/* Rate-limit hint */}
      {remaining <= 1 && (
        <div style={{
          padding: '6px 14px', fontSize: 11, color: t.textMuted, textAlign: 'center',
          fontFamily: t.fontMono, background: t.surface2,
        }}>
          ⓘ 아티스트 답장 전 최대 3개까지 · 남음 {remaining}개
        </div>
      )}

      {/* Input */}
      <div style={{
        padding: '10px 12px 28px', borderTop: `1px solid ${t.line}`,
        display: 'flex', gap: 8, alignItems: 'center',
      }}>
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && send()}
          disabled={remaining <= 0}
          placeholder={remaining <= 0 ? '메시지 제한에 도달했어요' : '메시지를 입력하세요'}
          style={{
            flex: 1, padding: '12px 16px', borderRadius: 22,
            border: `1px solid ${t.line}`, background: t.surface, color: t.text,
            fontSize: 14, fontFamily: t.font,
          }}
        />
        <button onClick={send} disabled={remaining <= 0} style={{
          width: 42, height: 42, borderRadius: '50%', border: 'none',
          background: remaining > 0 ? t.gradient : t.surface2,
          color: '#fff', fontSize: 18, cursor: remaining > 0 ? 'pointer' : 'not-allowed',
        }}>↑</button>
      </div>
    </div>
  );
}

function CommunityScreen({ t, artistId, onBack }) {
  const a = ARTISTS.find(x => x.id === artistId) || ARTISTS[0];
  const [tab, setTab] = React.useState('fan');
  const posts = FEED_POSTS.filter(p => p.artistId === a.id);

  return (
    <div style={{ height: '100%', overflowY: 'auto', paddingTop: 60, paddingBottom: 96 }}>
      <div style={{ padding: '0 20px 14px' }}>
        <button onClick={onBack} style={{
          background: 'transparent', border: 'none', color: t.text, fontSize: 20, cursor: 'pointer',
          padding: 0, marginBottom: 8,
        }}>←</button>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <ArtistAvatar artist={a} size={44} t={t}/>
          <div>
            <div style={{ fontFamily: t.fontDisplay, fontSize: 24, fontWeight: 800, letterSpacing: -0.6 }}>{a.name}</div>
            <div style={{ fontSize: 12, color: t.textDim, fontFamily: t.fontMono }}>Fan Community</div>
          </div>
        </div>
      </div>

      <div style={{
        display: 'flex', gap: 4, padding: '0 16px', borderBottom: `1px solid ${t.line}`,
      }}>
        {['fan','artist','media','notice'].map(k => (
          <button key={k} onClick={() => setTab(k)} style={{
            flex: 1, padding: '10px 0', background: 'transparent', border: 'none',
            color: tab === k ? t.text : t.textDim,
            fontWeight: tab === k ? 800 : 600, fontSize: 13, cursor: 'pointer',
            borderBottom: tab === k ? `2px solid ${t.accent}` : '2px solid transparent',
            fontFamily: t.font, textTransform: 'uppercase', letterSpacing: 0.5,
          }}>{k}</button>
        ))}
      </div>

      <div style={{ padding: '12px 16px' }}>
        {/* Composer */}
        <div style={{
          display: 'flex', gap: 10, padding: 12, borderRadius: 16,
          background: t.surface, border: `1px solid ${t.line}`, marginBottom: 12,
        }}>
          <div style={{ width: 36, height: 36, borderRadius: '50%', background: t.chip,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 800, color: t.accent, fontSize: 14 }}>나</div>
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', color: t.textMuted, fontSize: 13 }}>
            {tab === 'fan' ? '팬 포스트를 작성해 보세요…' : tab === 'artist' ? 'Fan Letter 작성 (Membership 필요)' : '#해시태그로 공유'}
          </div>
          <button style={{
            padding: '6px 12px', borderRadius: 10, border: 'none',
            background: t.gradient, color: '#fff', fontWeight: 800, fontSize: 11, cursor: 'pointer',
          }}>POST</button>
        </div>

        {/* Posts */}
        {(tab === 'fan' ? [
          { author: '별빛모아', role: 'FAN', body: '오늘 데뷔 2주년 축하해요! 2년 동안 함께해서 정말 행복했어요 💜', likes: 4812, comments: 428, img: true },
          { author: 'luminous', role: 'FAN', body: '@YUNA 언니 헤어 진짜 대박이에요... 오늘 라이브 꼭 챙겨볼게요!!', likes: 892, comments: 67 },
          { author: 'starling', role: 'FAN', body: '#lumen8_prism 해시태그 트렌딩 중 🔥', likes: 1_204, comments: 88, tag: true },
        ] : tab === 'artist' ? posts.filter(p => p.type === 'artist').map(p => ({ author: p.author, role: 'ARTIST', body: p.body, likes: p.likes, comments: p.comments, artist: true })) : [
          { author: '루멘기록', role: 'FAN', body: '어제 앵콜 무대 팬캠 공유합니다 🎬', likes: 2110, comments: 190, media: true },
        ]).map((p, i) => (
          <div key={i} style={{
            padding: 14, borderRadius: 16, background: t.surface,
            border: p.artist ? `1px solid ${t.accent}44` : `1px solid ${t.line}`,
            marginBottom: 10,
          }}>
            <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 8 }}>
              <div style={{ width: 32, height: 32, borderRadius: '50%',
                background: p.artist ? t.gradient : t.chip,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: p.artist ? '#fff' : t.accent, fontWeight: 800, fontSize: 12,
              }}>{p.author.slice(0,2)}</div>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                  <span style={{ fontWeight: 700, fontSize: 13 }}>{p.author}</span>
                  {p.artist && <span style={{ fontSize: 9, fontWeight: 800, padding: '2px 5px', borderRadius: 3, background: t.gradient, color: '#fff' }}>✓</span>}
                </div>
                <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono }}>{p.role} · 방금 전</div>
              </div>
            </div>
            <div style={{ fontSize: 13.5, lineHeight: 1.5, color: t.text }}>{p.body}</div>
            {p.img && <div style={{ marginTop: 10 }}><MediaPlaceholder artist={a} kind="IMAGE" label="Anniversary cake 🎂" aspect="4/3" t={t} radius={12}/></div>}
            {p.media && <div style={{ marginTop: 10 }}><MediaPlaceholder artist={a} kind="VIDEO" label="Encore fancam" aspect="16/9" t={t} radius={12}/></div>}
            {p.tag && (
              <div style={{ display: 'flex', gap: 6, marginTop: 8, flexWrap: 'wrap' }}>
                {['#lumen8_prism','#yuna','#comeback'].map(tg => (
                  <span key={tg} style={{
                    fontSize: 11, padding: '3px 10px', borderRadius: 12, background: t.chip, color: t.accent, fontWeight: 700,
                    fontFamily: t.fontMono,
                  }}>{tg}</span>
                ))}
              </div>
            )}
            <div style={{ display: 'flex', gap: 18, marginTop: 10, fontSize: 12, color: t.textDim }}>
              <span>♡ {p.likes?.toLocaleString()}</span>
              <span>💬 {p.comments?.toLocaleString()}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { LiveScreen, DMListScreen, DMThreadScreen, CommunityScreen });

// Connectfin — Desktop artist profile (8 tabs)

// ── API 응답 → 기존 카드 컴포넌트 호환 포맷 변환 ──
function mapFanPost(fp) {
  return {
    id: fp.fanPostId,
    artistId: fp.artistId,
    author: fp.writerNickname,
    body: fp.content,
    likes: window.ConnectfinAPI.formatCount(fp.likeCount),
    likeCount: fp.likeCount,
    comments: fp.commentCount,
    timeAgo: window.ConnectfinAPI.formatTime(fp.createdAt),
    hasGrid: fp.mediaCount > 0,
    gridCount: Math.min(fp.mediaCount, 6),
    // API 전용 필드 (카드에서 선택적 사용)
    mediaCount: fp.mediaCount,
    media: fp.media || [],
    hashtags: fp.hashtags || [],
    fanMembershipSubscribed: fp.fanMembershipSubscribed,
    dmSubscribed: fp.dmSubscribed,
    writerProfileImageUrl: fp.writerProfileImageUrl,
    writerId: fp.writerId,
  };
}

function mapArtistPost(ap) {
  return {
    id: ap.artistPostId,
    artistId: ap.artistId,
    author: ap.writerNickname,
    body: ap.content,
    likes: window.ConnectfinAPI.formatCount(ap.likeCount),
    comments: ap.commentCount,
    time: window.ConnectfinAPI.formatTime(ap.createdAt),
    // API 전용 필드
    artistPostId: ap.artistPostId,
    likeCount: ap.likeCount,
    commentCount: ap.commentCount,
    mediaCount: ap.mediaCount,
    media: ap.media || [],
    hashtags: ap.hashtags || [],
    artistBadge: ap.artistBadge,
    writerProfileImageUrl: ap.writerProfileImageUrl,
    writerId: ap.writerId,
  };
}

function mapFanLetterListItem(fl) {
  return {
    id: fl.fanLetterId,
    artistId: null, // 목록 응답에 없음
    recipientType: fl.recipientType,
    recipientDisplayName: fl.recipientDisplayName,
    recipientProfileImageUrl: fl.recipientProfileImageUrl,
    imageUrl: fl.image?.imageUrl || null,
    thumbnailUrl: fl.image?.thumbnailUrl || null,
    artistLiked: fl.artistLiked,
    artistLikeDisplayName: fl.artistLikeDisplayName,
    artistLikeProfileImageUrl: fl.artistLikeProfileImageUrl,
    createdAt: fl.createdAt,
    // mock 호환 필드
    author: fl.recipientDisplayName,
    body: null, // FanLetter는 텍스트 없음
    texture: null, // API 응답에는 텍스처 없음 — 이미지 기반
  };
}

// ── 공통 좋아요 버튼 ──
// postType: 'FAN_POST' | 'ARTIST_POST' | 'FAN_LETTER'
function LikeButton({ t, postType, artistId, postId, initialCount }) {
  const [liked, setLiked] = React.useState(false);
  const [count, setCount] = React.useState(initialCount || 0);
  const [toggling, setToggling] = React.useState(false);

  const toggle = async () => {
    if (toggling || !window.ConnectfinAPI.getToken()) return;
    setToggling(true);

    // Optimistic UI: 즉시 반영
    const nextLiked = !liked;
    setLiked(nextLiked);
    setCount(prev => prev + (nextLiked ? 1 : -1));

    try {
      let path;
      if (postType === 'FAN_POST') {
        path = `/api/post/v1/artists/${artistId}/fan-posts/${postId}/likes/toggle`;
      } else if (postType === 'ARTIST_POST') {
        path = `/api/post/v3/artists/${artistId}/artist-posts/${postId}/likes/toggle`;
      } else if (postType === 'FAN_LETTER') {
        path = `/api/post/v1/artists/${artistId}/fan-letters/${postId}/likes/toggle`;
      }

      const res = await window.ConnectfinAPI.api(path, { method: 'POST' });

      // 서버 응답으로 보정
      if (res.reacted !== undefined) {
        // v1 동기 (InteractionResponse): 서버 확정값
        setLiked(res.reacted);
        setCount(res.reactionCount);
      } else if (res.expectedReacted !== undefined) {
        // v3 비동기 (ArtistPostLikeQueuedResponse): 의도 상태
        setLiked(res.expectedReacted);
        // count는 서버가 안 줌 — optimistic 값 유지
      }
    } catch (err) {
      // 실패 시 롤백
      setLiked(!nextLiked);
      setCount(prev => prev + (nextLiked ? -1 : 1));
    } finally {
      setToggling(false);
    }
  };

  return (
    <button onClick={toggle} disabled={toggling} style={{
      padding: '10px 16px', borderRadius: 10,
      border: liked ? 'none' : `1px solid ${t.line}`,
      background: liked ? t.chip : 'transparent',
      color: liked ? t.accent : t.text,
      cursor: 'pointer', fontWeight: 700, fontSize: 12,
      display: 'flex', alignItems: 'center', gap: 6,
      opacity: toggling ? 0.6 : 1,
    }}>
      {liked ? '♥' : '♡'} {window.ConnectfinAPI.formatCount(count)}
    </button>
  );
}

function mapLiveReplay(live) {
  return {
    id: live.id || live.liveId,
    artistId: live.artistId,
    title: live.title,
    duration: live.durationSeconds ? formatDuration(live.durationSeconds) : (live.duration || ''),
    date: window.ConnectfinAPI.formatTime(live.replayPublishedAt || live.createdAt),
    plays: '',
    likes: '',
    comments: 0,
    voiceOnly: false,
    membership: false,
    liveStatus: live.liveStatus || 'ENDED',
    thumbnailUrl: live.thumbnailUrl,
    replayUrl: live.replayUrl,
    hostDisplayName: live.hostDisplayName,
  };
}

function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '0:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function ArtistMemberChip({ member, artist, t }) {
  const [followed, setFollowed] = React.useState(false);
  const [toggling, setToggling] = React.useState(false);

  const toggleFollow = async (e) => {
    e.stopPropagation();
    if (toggling || !window.ConnectfinAPI.getToken()) return;
    setToggling(true);
    try {
      const res = await window.ConnectfinAPI.api(
        `/api/member/v1/follows/artist-members/${member.artistMemberId}/toggle`,
        { method: 'POST' }
      );
      setFollowed(res.followed);
    } catch (err) {
      // 무시
    } finally {
      setToggling(false);
    }
  };

  return (
    <div style={{
      flexShrink: 0, display: 'flex', alignItems: 'center', gap: 6,
      padding: '4px 10px 4px 4px', borderRadius: 20,
      background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(8px)',
      color: '#fff', fontSize: 11, fontWeight: 600,
    }}>
      <div style={{
        width: 24, height: 24, borderRadius: '50%',
        background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 9, fontWeight: 800, color: '#fff',
      }}>{member.stageName?.slice(0, 1)}</div>
      <span>{member.stageName}</span>
      <button onClick={toggleFollow} disabled={toggling} style={{
        marginLeft: 2, padding: '2px 8px', borderRadius: 10, border: 'none',
        background: followed ? 'rgba(255,255,255,0.3)' : 'rgba(255,255,255,0.15)',
        color: '#fff', fontSize: 10, fontWeight: 700, cursor: 'pointer',
        opacity: toggling ? 0.5 : 1,
      }}>{followed ? '팔로잉' : '팔로우'}</button>
    </div>
  );
}

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
          {artist.intro || '가입하고 최신 소식을 받아보세요!'}
        </div>
      </div>
      <button style={{
        position: 'absolute', bottom: 20, right: 24,
        padding: '8px 16px', borderRadius: 10, border: 'none',
        background: '#fff', color: '#111', fontWeight: 800, fontSize: 13, cursor: 'pointer',
      }}>가입하기</button>

      {/* Artist Members */}
      {artist.artistMembers && artist.artistMembers.length > 0 && (
        <div style={{
          position: 'absolute', top: 12, left: 24, right: 24,
          display: 'flex', gap: 8, overflowX: 'auto',
        }}>
          {artist.artistMembers.slice().sort((a, b) => a.sortOrder - b.sortOrder).map(m => (
            <ArtistMemberChip key={m.artistMemberId} member={m} artist={artist} t={t}/>
          ))}
        </div>
      )}
    </div>
  );
}

// Main desktop profile router
function DesktopArtistProfile({ t, theme, artist, tab, onNavProfile, onOpenLive, onOpenDM, onOpenMembership }) {
  const [artistDetail, setArtistDetail] = React.useState(null);
  const [activeLive, setActiveLive] = React.useState(null);

  React.useEffect(() => {
    window.ConnectfinAPI.api(`/api/member/v2/artists/${artist.id}`)
      .then(data => setArtistDetail(data))
      .catch(err => { console.warn('API fallback to mock:', err?.message || err); });
  }, [artist.id]);

  // 진행 중인 라이브 폴링 (10초마다) — 어드민이 시작/종료 시 반영
  React.useEffect(() => {
    let cancelled = false;
    const fetchLive = () => {
      window.ConnectfinAPI.api(`/api/v1/artists/${artist.id}/lives`)
        .then(data => {
          if (cancelled) return;
          const live = (Array.isArray(data) ? data : []).find(l => (l.liveStatus || l.status) === 'LIVE');
          setActiveLive(live || null);
        })
        .catch(() => {});
    };
    fetchLive();
    const id = setInterval(fetchLive, 10000);
    return () => { cancelled = true; clearInterval(id); };
  }, [artist.id]);

  const displayArtist = artistDetail ? {
    ...artist,
    name: artistDetail.name,
    intro: artistDetail.intro,
    coverImageUrl: artistDetail.coverImageUrl,
    profileImageUrl: artistDetail.profileImageUrl,
    artistMembers: artistDetail.artistMembers || [],
  } : artist;

  let body;
  if (tab === 'highlight') body = <TabHighlight t={t} theme={theme} artist={displayArtist} onNavProfile={onNavProfile}/>;
  else if (tab === 'fan') body = <TabFan t={t} theme={theme} artist={displayArtist}/>;
  else if (tab === 'artist') body = <TabArtistPosts t={t} theme={theme} artist={displayArtist}/>;
  else if (tab === 'fanletter') body = <TabFanLetter t={t} theme={theme} artist={displayArtist}/>;
  else if (tab === 'media') body = <TabMedia t={t} theme={theme} artist={displayArtist}/>;
  else if (tab === 'live') body = <TabLive t={t} theme={theme} artist={displayArtist} onOpenLive={onOpenLive}/>;
  else if (tab === 'notice') body = <TabNotice t={t} theme={theme} artist={displayArtist}/>;
  else if (tab === 'shop') body = <TabShop t={t} theme={theme} artist={displayArtist}/>;
  else if (tab === 'admin') body = <TabAdmin t={t} theme={theme} artist={displayArtist}/>;

  return (
    <div>
      {activeLive && (
        <button onClick={() => onOpenLive(artist.id)} style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: 14,
          padding: '12px 28px', border: 'none', cursor: 'pointer', textAlign: 'left',
          background: t.liveGradient || 'linear-gradient(90deg, #FF2D55, #FF6B9F)',
          color: '#fff', fontFamily: t.font,
        }}>
          <span style={{
            width: 8, height: 8, borderRadius: '50%', background: '#fff',
            animation: 'connectfin-pulse 1.2s ease-in-out infinite', flexShrink: 0,
          }}/>
          <span style={{ fontWeight: 800, fontSize: 11, letterSpacing: 1.2, fontFamily: t.fontMono }}>LIVE NOW</span>
          <span style={{ fontWeight: 700, fontSize: 14, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {activeLive.title || `${artist.name} 라이브 진행 중`}
          </span>
          <span style={{ fontSize: 12, opacity: 0.95, fontWeight: 700 }}>지금 입장 →</span>
        </button>
      )}
      <ArtistCoverBanner artist={displayArtist} t={t} theme={theme}/>
      <div style={{ display: 'flex', maxWidth: 1320, margin: '0 auto', padding: '20px 24px 60px', gap: 24 }}>
        <div style={{ flex: 1, minWidth: 0 }}>{body}</div>
        <DesktopRightSidebar t={t} theme={theme} artist={displayArtist} onOpenDM={onOpenDM} onOpenMembership={onOpenMembership}/>
      </div>
    </div>
  );
}

// ────────── HIGHLIGHT (summary) ──────────
function TabHighlight({ t, theme, artist, onNavProfile }) {
  const notices = NOTICES.filter(n => n.artistId === artist.id).slice(0, 5);
  const mockArtistPost = ARTIST_POSTS.find(p => p.artistId === artist.id);
  const mockFanPosts = FAN_POSTS.filter(p => p.artistId === artist.id).slice(2, 8);

  const [artistPost, setArtistPost] = React.useState(mockArtistPost);
  const [fanPosts, setFanPosts] = React.useState(mockFanPosts);
  const [hotLetters, setHotLetters] = React.useState([]);

  React.useEffect(() => {
    window.ConnectfinAPI.api(`/api/member/v1/artists/${artist.id}/dashboard`)
      .then(data => {
        if (data.latestArtistPost) {
          setArtistPost(mapArtistPost(data.latestArtistPost));
        }
        if (data.hotFanPosts && data.hotFanPosts.length > 0) {
          setFanPosts(data.hotFanPosts.map(mapFanPost));
        }
        if (data.hotFanLetters && data.hotFanLetters.length > 0) {
          setHotLetters(data.hotFanLetters);
        }
      })
      .catch(err => { console.warn('API fallback to mock:', err?.message || err); });
  }, [artist.id]);

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

      {/* Hot Fan Letters */}
      {hotLetters.length > 0 && (
        <Section t={t} theme={theme}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <div style={{ fontWeight: 800, fontSize: 15 }}>🔥 Hot Fan Letters</div>
            <button onClick={() => onNavProfile('fanletter')} style={linkBtn(t)}>더 보기 →</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 10 }}>
            {hotLetters.slice(0, 4).map(fl => (
              <div key={fl.fanLetterId} style={{
                borderRadius: 10, overflow: 'hidden', aspectRatio: '3/4',
                background: fl.image?.imageUrl ? 'transparent' : `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`,
                position: 'relative',
              }}>
                {fl.image?.imageUrl ? (
                  <img src={fl.image.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }}/>
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#fff', fontSize: 24 }}>✉</div>
                )}
                {fl.artistLiked && (
                  <div style={{
                    position: 'absolute', bottom: 6, right: 6,
                    width: 22, height: 22, borderRadius: '50%',
                    background: 'rgba(255,255,255,0.9)', display: 'flex',
                    alignItems: 'center', justifyContent: 'center', fontSize: 11,
                  }}>💜</div>
                )}
                <div style={{
                  position: 'absolute', bottom: 0, left: 0, right: 0,
                  padding: '4px 8px', fontSize: 10, color: '#fff',
                  background: 'linear-gradient(transparent, rgba(0,0,0,0.5))',
                }}>♡ {window.ConnectfinAPI.formatCount(fl.likeCount)}</div>
              </div>
            ))}
          </div>
        </Section>
      )}
    </div>
  );
}

// ────────── FAN (팬 포스트 타임라인) ──────────
function TabFan({ t, theme, artist }) {
  const mockPosts = FAN_POSTS.filter(p => p.artistId === artist.id);
  const [posts, setPosts] = React.useState(mockPosts);
  const [loading, setLoading] = React.useState(false);
  const [hasNext, setHasNext] = React.useState(false);
  const [nextCursor, setNextCursor] = React.useState(null);

  const [activeTab, setActiveTab] = React.useState('latest'); // 'latest' | 'hot'
  const [hotPosts, setHotPosts] = React.useState([]);
  const [hotHasNext, setHotHasNext] = React.useState(false);
  const [hotNextScoreCursor, setHotNextScoreCursor] = React.useState(null);
  const [hotNextIdCursor, setHotNextIdCursor] = React.useState(null);

  const [showWriteForm, setShowWriteForm] = React.useState(false);
  const [writeContent, setWriteContent] = React.useState('');
  const [writeSubmitting, setWriteSubmitting] = React.useState(false);

  const submitFanPost = async () => {
    if (!writeContent.trim() || writeSubmitting) return;
    if (!window.ConnectfinAPI.getToken()) { alert('로그인이 필요합니다.'); return; }
    setWriteSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('content', writeContent.trim());
      await window.ConnectfinAPI.apiMultipart(`/api/post/v1/artists/${artist.id}/fan-posts`, formData);
      setWriteContent('');
      setShowWriteForm(false);
      window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-posts`)
        .then(data => { setPosts(data.content.map(mapFanPost)); setHasNext(data.hasNext); setNextCursor(data.nextCursor); })
        .catch(err => { console.warn('Refresh failed:', err?.message || err); });
    } catch (err) {
      alert('작성 실패: ' + (err.message || '알 수 없는 오류'));
    } finally {
      setWriteSubmitting(false);
    }
  };

  React.useEffect(() => {
    setLoading(true);
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-posts`)
      .then(data => {
        setPosts(data.content.map(mapFanPost));
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
      })
      .catch(err => { console.warn('API fallback to mock:', err?.message || err); })
      .finally(() => setLoading(false));
  }, [artist.id]);

  const loadMore = () => {
    if (!hasNext || !nextCursor || loading) return;
    setLoading(true);
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-posts?cursor=${nextCursor}`)
      .then(data => {
        setPosts(prev => [...prev, ...data.content.map(mapFanPost)]);
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
      })
      .catch(err => { console.warn('API error suppressed:', err?.message || err); })
      .finally(() => setLoading(false));
  };

  const loadHot = (append = false) => {
    setLoading(true);
    let path = `/api/post/v1/artists/${artist.id}/fan-posts/hot`;
    if (append && hotNextScoreCursor != null) {
      path += `?scoreCursor=${hotNextScoreCursor}&idCursor=${hotNextIdCursor}`;
    }
    window.ConnectfinAPI.api(path)
      .then(data => {
        const mapped = data.content.map(mapFanPost);
        if (append) {
          setHotPosts(prev => [...prev, ...mapped]);
        } else {
          setHotPosts(mapped);
        }
        setHotHasNext(data.hasNext);
        setHotNextScoreCursor(data.nextScoreCursor);
        setHotNextIdCursor(data.nextIdCursor);
      })
      .catch(err => { console.warn('API error suppressed:', err?.message || err); })
      .finally(() => setLoading(false));
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 14 }}>
        <Chip t={t} active={activeTab === 'latest'} onClick={() => setActiveTab('latest')}>전체</Chip>
        <Chip t={t} active={activeTab === 'hot'} onClick={() => { setActiveTab('hot'); if (hotPosts.length === 0) loadHot(); }}><span>🔥</span> Hot</Chip>
      </div>
      {!showWriteForm ? (
        <button onClick={() => setShowWriteForm(true)} style={{
          width: '100%', padding: '12px 0', marginBottom: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>✏️ 팬포스트 작성</button>
      ) : (
        <div style={{ padding: 14, marginBottom: 12, borderRadius: 12, border: `1px solid ${t.accent}`, background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface }}>
          <textarea value={writeContent} onChange={e => setWriteContent(e.target.value)}
            placeholder="팬포스트를 작성해 보세요..." maxLength={5000}
            style={{ width: '100%', minHeight: 80, padding: 10, borderRadius: 8, border: `1px solid ${t.line}`, background: 'transparent', color: t.text, fontSize: 13, fontFamily: t.font, resize: 'vertical' }}/>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
            <span style={{ fontSize: 11, color: t.textDim }}>{writeContent.length} / 5000</span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button onClick={() => { setShowWriteForm(false); setWriteContent(''); }} style={{ padding: '6px 14px', borderRadius: 8, border: `1px solid ${t.line}`, background: 'transparent', color: t.textDim, fontSize: 12, cursor: 'pointer' }}>취소</button>
              <button onClick={submitFanPost} disabled={writeSubmitting || !writeContent.trim()} style={{ padding: '6px 14px', borderRadius: 8, border: 'none', background: t.accent, color: '#fff', fontSize: 12, fontWeight: 600, cursor: 'pointer', opacity: writeSubmitting || !writeContent.trim() ? 0.5 : 1 }}>{writeSubmitting ? '작성 중...' : '게시'}</button>
            </div>
          </div>
        </div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {(activeTab === 'latest' ? posts : hotPosts).map(p => (
          <FanPostFullCard key={p.id} post={p} artist={artist} t={t} theme={theme}
            authUser={window.__connectfinAuthUser}
            onDeleted={(deletedId) => {
              setPosts(prev => prev.filter(x => x.id !== deletedId));
              setHotPosts(prev => prev.filter(x => x.id !== deletedId));
            }}
          />
        ))}
      </div>
      {activeTab === 'latest' && hasNext && (
        <button onClick={loadMore} disabled={loading} style={{
          width: '100%', padding: '12px 0', marginTop: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>{loading ? '로딩 중...' : '더 보기'}</button>
      )}
      {activeTab === 'hot' && hotHasNext && (
        <button onClick={() => loadHot(true)} disabled={loading} style={{
          width: '100%', padding: '12px 0', marginTop: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>{loading ? '로딩 중...' : '더 보기'}</button>
      )}
    </div>
  );
}

// ────────── ARTIST (아티스트 포스트 타임라인) ──────────
function TabArtistPosts({ t, theme, artist }) {
  const mockPosts = ARTIST_POSTS.filter(p => p.artistId === artist.id);
  const [posts, setPosts] = React.useState(mockPosts);
  const [loading, setLoading] = React.useState(false);
  const [hasNext, setHasNext] = React.useState(false);
  const [nextCursor, setNextCursor] = React.useState(null);

  React.useEffect(() => {
    setLoading(true);
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/artist-posts`)
      .then(data => {
        setPosts(data.content.map(mapArtistPost));
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
      })
      .catch(err => { console.warn('API fallback to mock:', err?.message || err); })
      .finally(() => setLoading(false));
  }, [artist.id]);

  const loadMore = () => {
    if (!hasNext || !nextCursor || loading) return;
    setLoading(true);
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/artist-posts?cursor=${nextCursor}`)
      .then(data => {
        setPosts(prev => [...prev, ...data.content.map(mapArtistPost)]);
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
      })
      .catch(err => { console.warn('API error suppressed:', err?.message || err); })
      .finally(() => setLoading(false));
  };

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
      {hasNext && (
        <button onClick={loadMore} disabled={loading} style={{
          width: '100%', padding: '12px 0', marginTop: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>{loading ? '로딩 중...' : '더 보기'}</button>
      )}
    </div>
  );
}

// ────────── FAN LETTER (텍스처 카드 그리드) ──────────
function TabFanLetter({ t, theme, artist }) {
  const mockLetters = FAN_LETTERS.filter(l => l.artistId === artist.id);
  const [letters, setLetters] = React.useState(mockLetters);
  const [selected, setSelected] = React.useState(null);
  const [selectedDetail, setSelectedDetail] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [hasNext, setHasNext] = React.useState(false);
  const [nextCursor, setNextCursor] = React.useState(null);

  const [activeTab, setActiveTab] = React.useState('latest');
  const [hotLetters, setHotLetters] = React.useState([]);
  const [hotHasNext, setHotHasNext] = React.useState(false);
  const [hotNextOffset, setHotNextOffset] = React.useState(null);

  const [showLetterForm, setShowLetterForm] = React.useState(false);
  const [letterSubmitting, setLetterSubmitting] = React.useState(false);
  const [letterRecipientType, setLetterRecipientType] = React.useState('ARTIST');
  const [letterRecipientArtistMemberId, setLetterRecipientArtistMemberId] = React.useState(null);
  const [letterImage, setLetterImage] = React.useState(null);

  React.useEffect(() => {
    const artistMembers = (artist.artistMembers || []).slice().sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
    if (letterRecipientType !== 'ARTIST_MEMBER') {
      if (letterRecipientArtistMemberId != null) {
        setLetterRecipientArtistMemberId(null);
      }
      return;
    }
    if (artistMembers.length === 0) {
      if (letterRecipientArtistMemberId != null) {
        setLetterRecipientArtistMemberId(null);
      }
      return;
    }
    const hasCurrent = artistMembers.some(member => String(member.artistMemberId) === String(letterRecipientArtistMemberId));
    if (!hasCurrent) {
      setLetterRecipientArtistMemberId(artistMembers[0].artistMemberId);
    }
  }, [artist.artistMembers, letterRecipientArtistMemberId, letterRecipientType]);

  const submitFanLetter = async () => {
    if (letterSubmitting) return;
    if (!window.ConnectfinAPI.getToken()) { alert('로그인이 필요합니다.'); return; }
    if (!letterImage) { alert('팬레터는 이미지를 한 장 선택해야 합니다.'); return; }
    if (letterRecipientType === 'ARTIST_MEMBER' && !letterRecipientArtistMemberId) {
      alert('받는 아티스트 멤버를 선택해 주세요.');
      return;
    }
    setLetterSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('recipientType', letterRecipientType);
      if (letterRecipientType === 'ARTIST_MEMBER') {
        formData.append('recipientArtistMemberId', String(letterRecipientArtistMemberId));
      }
      formData.append('image', letterImage);
      await window.ConnectfinAPI.apiMultipart(`/api/post/v1/artists/${artist.id}/fan-letters`, formData);
      setLetterImage(null);
      setLetterRecipientType('ARTIST');
      setLetterRecipientArtistMemberId(null);
      setShowLetterForm(false);
      window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-letters`)
        .then(data => { setLetters(data.content.map(mapFanLetterListItem)); setHasNext(data.hasNext); setNextCursor(data.nextCursor); })
        .catch(err => { console.warn('Refresh failed:', err?.message || err); });
    } catch (err) {
      const debugSuffix = [err?.code, err?.status].filter(Boolean).join(' / ');
      alert('작성 실패: ' + (err.message || '멤버십 구독이 필요합니다') + (debugSuffix ? ` (${debugSuffix})` : ''));
    } finally {
      setLetterSubmitting(false);
    }
  };

  const loadHotLetters = (append = false) => {
    setLoading(true);
    let path = `/api/post/v1/artists/${artist.id}/fan-letters/hot`;
    if (append && hotNextOffset != null) {
      path += `?offset=${hotNextOffset}`;
    }
    window.ConnectfinAPI.api(path)
      .then(data => {
        const mapped = data.content.map(item => ({
          id: item.fanLetterId,
          recipientType: item.recipientType,
          recipientDisplayName: item.recipientDisplayName,
          recipientProfileImageUrl: item.recipientProfileImageUrl,
          imageUrl: item.image?.imageUrl || null,
          thumbnailUrl: item.image?.thumbnailUrl || null,
          artistLiked: item.artistLiked,
          artistLikeDisplayName: item.artistLikeDisplayName,
          artistLikeProfileImageUrl: item.artistLikeProfileImageUrl,
          createdAt: item.createdAt,
          likeCount: item.likeCount,
          author: item.recipientDisplayName,
          body: null,
          texture: null,
        }));
        if (append) {
          setHotLetters(prev => [...prev, ...mapped]);
        } else {
          setHotLetters(mapped);
        }
        setHotHasNext(data.hasNext);
        setHotNextOffset(data.nextOffset);
      })
      .catch(err => { console.warn('API error suppressed:', err?.message || err); })
      .finally(() => setLoading(false));
  };

  React.useEffect(() => {
    if (!window.ConnectfinAPI.getToken()) return; // FanLetter 목록은 로그인 필수
    setLoading(true);
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-letters`)
      .then(data => {
        setLetters(data.content.map(mapFanLetterListItem));
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
      })
      .catch(err => { console.warn('API fallback to mock:', err?.message || err); })
      .finally(() => setLoading(false));
  }, [artist.id]);

  const openDetail = (letter) => {
    setSelected(letter);
    // 상세 API 호출 (목록과 필드가 다름)
    if (window.ConnectfinAPI.getToken() && letter.id) {
      window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-letters/${letter.id}`)
        .then(data => setSelectedDetail(data))
        .catch(err => { console.warn('Fan letter detail load failed:', err?.message || err); setSelectedDetail(null); });
    }
  };

  const closeDetail = () => {
    setSelected(null);
    setSelectedDetail(null);
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 14 }}>
        <Chip t={t} active={activeTab === 'latest'} onClick={() => setActiveTab('latest')}>전체</Chip>
        <Chip t={t} active={activeTab === 'hot'} onClick={() => { setActiveTab('hot'); if (hotLetters.length === 0) loadHotLetters(); }}><span>🔥</span> Hot</Chip>
        <div style={{ flex: 1 }}/>
        <Chip t={t}>🌐 한국어</Chip>
      </div>
      {!showLetterForm ? (
        <button onClick={() => setShowLetterForm(true)} style={{
          width: '100%', padding: '12px 0', marginBottom: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>💌 팬레터 보내기</button>
      ) : (
        <div style={{ padding: 14, marginBottom: 12, borderRadius: 12, border: `1px solid ${t.accent}`, background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface }}>
          <div style={{ fontSize: 12, color: t.textDim, marginBottom: 8 }}>받는 대상</div>
          <div style={{ display: 'flex', gap: 6, marginBottom: 12 }}>
            {['ARTIST', 'ARTIST_MEMBER'].map(rt => (
              <button key={rt} onClick={() => setLetterRecipientType(rt)} style={{
                padding: '6px 12px', borderRadius: 8, fontSize: 12, cursor: 'pointer', fontFamily: t.font,
                border: letterRecipientType === rt ? `1px solid ${t.accent}` : `1px solid ${t.line}`,
                background: letterRecipientType === rt ? t.accent : 'transparent',
                color: letterRecipientType === rt ? '#fff' : t.text,
              }}>{rt === 'ARTIST' ? '그룹 전체' : '멤버 지정'}</button>
            ))}
          </div>
          {letterRecipientType === 'ARTIST_MEMBER' && (
            <div style={{ marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: t.textDim, marginBottom: 8 }}>받는 멤버</div>
              <select
                value={letterRecipientArtistMemberId || ''}
                onChange={e => setLetterRecipientArtistMemberId(e.target.value ? Number(e.target.value) : null)}
                disabled={!artist.artistMembers || artist.artistMembers.length === 0}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: 8,
                  border: `1px solid ${t.line}`,
                  background: 'transparent',
                  color: t.text,
                  fontSize: 12,
                  fontFamily: t.font,
                }}
              >
                {(!artist.artistMembers || artist.artistMembers.length === 0) && (
                  <option value="">선택 가능한 멤버가 없습니다</option>
                )}
                {(artist.artistMembers || [])
                  .slice()
                  .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
                  .map(member => (
                    <option key={member.artistMemberId} value={member.artistMemberId}>
                      {member.stageName}
                    </option>
                  ))}
              </select>
            </div>
          )}
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 14px', borderRadius: 8, border: `1px dashed ${t.line}`, cursor: 'pointer', fontSize: 12, color: t.textDim, marginBottom: 12 }}>
            📷 {letterImage ? letterImage.name : '이미지 선택 (필수)'}
            <input type="file" accept="image/*" onChange={e => setLetterImage(e.target.files[0] || null)} style={{ display: 'none' }}/>
          </label>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 10, color: t.textDim }}>팬 멤버십 구독자만 작성 가능 · 팬레터는 이미지 1장 필수</span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button onClick={() => { setShowLetterForm(false); setLetterImage(null); setLetterRecipientType('ARTIST'); setLetterRecipientArtistMemberId(null); }} style={{ padding: '6px 14px', borderRadius: 8, border: `1px solid ${t.line}`, background: 'transparent', color: t.textDim, fontSize: 12, cursor: 'pointer' }}>취소</button>
              <button onClick={submitFanLetter} disabled={letterSubmitting} style={{ padding: '6px 14px', borderRadius: 8, border: 'none', background: t.accent, color: '#fff', fontSize: 12, fontWeight: 600, cursor: 'pointer', opacity: letterSubmitting ? 0.5 : 1 }}>{letterSubmitting ? '전송 중...' : '보내기'}</button>
            </div>
          </div>
        </div>
      )}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        {(activeTab === 'latest' ? letters : hotLetters).map(l => <FanLetterCard key={l.id} letter={l} artist={artist} t={t} theme={theme} onClick={() => openDetail(l)}/>)}
      </div>
      {activeTab === 'latest' && hasNext && (
        <button onClick={() => {
          if (!hasNext || !nextCursor || loading) return;
          setLoading(true);
          window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-letters?cursor=${nextCursor}`)
            .then(data => {
              setLetters(prev => [...prev, ...data.content.map(mapFanLetterListItem)]);
              setHasNext(data.hasNext);
              setNextCursor(data.nextCursor);
            })
            .catch(err => { console.warn('API error suppressed:', err?.message || err); })
            .finally(() => setLoading(false));
        }} disabled={loading} style={{
          width: '100%', padding: '12px 0', marginTop: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>{loading ? '로딩 중...' : '더 보기'}</button>
      )}
      {activeTab === 'hot' && hotHasNext && (
        <button onClick={() => loadHotLetters(true)} disabled={loading} style={{
          width: '100%', padding: '12px 0', marginTop: 12, borderRadius: 10,
          border: `1px solid ${t.line}`, background: 'transparent',
          color: t.textDim, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: t.font,
        }}>{loading ? '로딩 중...' : '더 보기'}</button>
      )}

      {selected && <FanLetterModal letter={selected} detail={selectedDetail} artist={artist} t={t} theme={theme} onClose={closeDetail}/>}
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
        aspectRatio: '3/4', position: 'relative',
        overflow: 'hidden',
        ...(letter.imageUrl ? {} : { padding: '20px 18px', ...texture, fontSize: 11, lineHeight: 1.6, whiteSpace: 'pre-line' }),
      }}>
        {letter.imageUrl ? (
          <img src={letter.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }}/>
        ) : letter.body}
        {letter.artistLiked && (
          <div style={{
            position: 'absolute', bottom: 8, right: 8,
            width: 28, height: 28, borderRadius: '50%',
            background: 'rgba(255,255,255,0.9)', display: 'flex',
            alignItems: 'center', justifyContent: 'center', fontSize: 14,
            boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
          }}>💜</div>
        )}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 14px', fontSize: 12 }}>
        <ArtistAvatar artist={artist} size={20} t={t}/>
        <span style={{ color: t.textDim }}>To. {letter.recipientDisplayName || artist.name}</span>
        <span style={{ marginLeft: 'auto', color: t.textDim, fontFamily: t.fontMono, fontSize: 10 }}>
          {window.ConnectfinAPI.formatTime(letter.createdAt) || `· ${letter.author || ''}`}
        </span>
      </div>
    </div>
  );
}

function FanLetterModal({ letter, detail, artist, t, theme, onClose }) {
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
              <div style={{ fontWeight: 800, fontSize: 14 }}>To. {detail?.recipientDisplayName || letter.recipientDisplayName || artist.name}</div>
              <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>
                From. {detail?.writerNickname || letter.author || ''}
                {detail?.fanMembershipSubscribed && <span style={{ color: t.accent2, marginLeft: 4 }}>✓</span>}
              </div>
            </div>
          </div>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: t.text, fontSize: 22, cursor: 'pointer' }}>×</button>
        </div>
        {(letter.imageUrl || detail?.image?.imageUrl) ? (
          <div style={{ borderRadius: 12, overflow: 'hidden', margin: '10px 0' }}>
            <img src={detail?.image?.imageUrl || letter.imageUrl} alt="" style={{ width: '100%', borderRadius: 12 }}/>
          </div>
        ) : letter.body ? (
          <div style={{ whiteSpace: 'pre-line', lineHeight: 1.7, fontSize: 14 }}>
            {letter.body}
          </div>
        ) : null}
        <div style={{ display: 'flex', gap: 10, marginTop: 18, alignItems: 'center' }}>
          <LikeButton
            t={t}
            postType="FAN_LETTER"
            artistId={artist.id}
            postId={letter.id}
            initialCount={detail?.likeCount ?? 0}
          />
          {(detail?.artistLiked || letter.artistLiked) && (
            <span style={{ fontSize: 12, color: t.accent2, fontWeight: 600 }}>
              💜 {detail?.artistLikeDisplayName || letter.artistLikeDisplayName || artist.name}
            </span>
          )}
          <div style={{ flex: 1 }}/>
          <button style={{ padding: '10px 16px', borderRadius: 10, border: 'none', background: t.gradient, color: '#fff', cursor: 'pointer', fontWeight: 800, fontSize: 12 }}>🌐 번역 보기</button>
        </div>
      </div>
    </div>
  );
}

// ────────── MEDIA (최신 + 멤버십 2단) ──────────
function TabMedia({ t, theme, artist }) {
  const latest = MEDIA_EXTENDED.filter(m => m.artistId === artist.id).slice(0, 6);
  const membership = MEDIA_EXTENDED.filter(m => m.artistId === artist.id && m.membership).slice(0, 4);

  const [youtubeVideos, setYoutubeVideos] = React.useState([]);

  React.useEffect(() => {
    window.ConnectfinAPI.api(`/api/media/v1/artists/${artist.id}/youtube-videos`)
      .then(data => setYoutubeVideos(data.content || []))
      .catch(err => { console.warn('YouTube videos load failed:', err?.message || err); });
  }, [artist.id]);

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

      {youtubeVideos.length > 0 && (
        <Section t={t} theme={theme}>
          <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 14 }}>YouTube</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14 }}>
            {youtubeVideos.map(v => (
              <a key={v.id || v.youtubeVideoId} href={v.youtubeUrl} target="_blank" rel="noopener noreferrer"
                style={{ textDecoration: 'none', color: t.text }}>
                <div style={{ borderRadius: 10, overflow: 'hidden', border: `1px solid ${t.line}` }}>
                  {v.thumbnailUrl && (
                    <img src={v.thumbnailUrl} alt={v.title} style={{ width: '100%', aspectRatio: '16/9', objectFit: 'cover' }}/>
                  )}
                  <div style={{ padding: 8 }}>
                    <div style={{ fontSize: 12, fontWeight: 600, lineHeight: 1.4,
                      display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden'
                    }}>{v.title}</div>
                    <div style={{ fontSize: 10, color: t.textDim, marginTop: 4, fontFamily: t.fontMono }}>
                      {v.writerDisplayName}
                    </div>
                  </div>
                </div>
              </a>
            ))}
          </div>
        </Section>
      )}
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
  const mockReplays = LIVE_REPLAYS.filter(r => r.artistId === artist.id);
  const [replays, setReplays] = React.useState(mockReplays);

  React.useEffect(() => {
    window.ConnectfinAPI.api(`/api/v1/artists/${artist.id}/lives/vods`)
      .then(data => {
        if (data.content && data.content.length > 0) {
          setReplays(data.content.map(mapLiveReplay));
        }
      })
      .catch(err => { console.warn('API fallback to mock:', err?.message || err); });
  }, [artist.id]);

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

function Chip({ t, active, children, onClick }) {
  return (
    <button onClick={onClick} style={{
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
  const [commentInput, setCommentInput] = React.useState('');
  const [commentSubmitting, setCommentSubmitting] = React.useState(false);
  const [comments, setComments] = React.useState([]);
  const [showComments, setShowComments] = React.useState(false);
  const [expandedReplies, setExpandedReplies] = React.useState({});

  const artistId = post.artistId || artist.id;
  const artistPostId = post.artistPostId || post.id;

  const loadComments = () => {
    if (!artistPostId) return;
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artistId}/artist-posts/${artistPostId}`)
      .then(data => { if (data.comments?.content) setComments(data.comments.content); })
      .catch(err => { console.warn('Comments load failed:', err?.message || err); });
  };

  const submitComment = async () => {
    if (!commentInput.trim() || commentSubmitting) return;
    if (!window.ConnectfinAPI.getToken()) { alert('로그인이 필요합니다.'); return; }
    setCommentSubmitting(true);
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${artistId}/artist-posts/${artistPostId}/comments`,
        { method: 'POST', body: JSON.stringify({ content: commentInput.trim(), parentId: null }) }
      );
      setCommentInput('');
      loadComments();
    } catch (err) {
      alert('댓글 작성 실패: ' + (err.message || ''));
    } finally {
      setCommentSubmitting(false);
    }
  };

  const loadReplies = (commentId) => {
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artistId}/artist-posts/${artistPostId}/comments/${commentId}/replies`)
      .then(data => {
        setExpandedReplies(prev => ({ ...prev, [commentId]: data.content || [] }));
      })
      .catch(err => { console.warn('Replies load failed:', err?.message || err); });
  };

  const deleteComment = async (commentId) => {
    if (!confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${artistId}/artist-posts/${artistPostId}/comments/${commentId}`,
        { method: 'DELETE' }
      );
      loadComments();
    } catch (err) {
      alert('삭제 실패: ' + (err.message || ''));
    }
  };

  const toggleCommentLike = async (commentId) => {
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${artistId}/artist-posts/${artistPostId}/comments/${commentId}/likes/toggle`,
        { method: 'POST' }
      );
      loadComments();
    } catch (err) {
      console.warn('Comment like failed:', err?.message || err);
    }
  };

  return (
    <div style={{ padding: inline ? 0 : 18, border: inline ? 'none' : `1px solid ${t.line}`, borderRadius: 12, background: inline ? 'transparent' : 'transparent' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <ArtistAvatar artist={artist} size={36} t={t}/>
        <div>
          <div style={{ fontWeight: 800, fontSize: 14 }}>
            {post.author || artist.name}
            {post.artistBadge && <span style={{ marginLeft: 6, fontSize: 9, fontWeight: 800, padding: '2px 6px', borderRadius: 4, background: t.gradient, color: '#fff', letterSpacing: 0.4 }}>ARTIST ✓</span>}
            {post.artistBadge === undefined && <span style={{ marginLeft: 4, color: t.accent2 }}>.</span>}
          </div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>{post.time}</div>
        </div>
      </div>
      <div style={{ fontSize: 14, lineHeight: 1.6, whiteSpace: 'pre-line', color: t.text }}>{post.body}</div>
      <div style={{ display: 'flex', gap: 4, marginTop: 10, fontSize: 11, color: t.textDim }}>
        <span style={{ cursor: 'pointer', fontFamily: t.fontMono }}>번역 보기</span>
      </div>
      {post.hashtags && post.hashtags.length > 0 && (
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 8 }}>
          {post.hashtags.map(tag => (
            <span key={tag} style={{ color: t.accent, fontSize: 12, fontWeight: 600 }}>#{tag}</span>
          ))}
        </div>
      )}
      <div style={{ display: 'flex', gap: 16, marginTop: 12, fontSize: 12, color: t.textDim, alignItems: 'center' }}>
        {post.artistPostId ? (
          <LikeButton t={t} postType="ARTIST_POST" artistId={post.artistId || artist.id} postId={post.artistPostId} initialCount={post.likeCount ?? 0}/>
        ) : (
          <span>♡ {post.likes}</span>
        )}
        <span>💬 {post.comments}</span>
      </div>
      {artistPostId && !inline && (
        <div style={{ marginTop: 12, borderTop: `1px solid ${t.line}`, paddingTop: 10 }}>
          {post.comments > 0 && !showComments && (
            <button onClick={() => { setShowComments(true); loadComments(); }} style={{
              background: 'transparent', border: 'none', color: t.textDim, fontSize: 12, cursor: 'pointer', padding: 0, marginBottom: 8,
            }}>💬 댓글 {post.comments}개 보기</button>
          )}
          {showComments && comments.length > 0 && (
            <div style={{ marginBottom: 10 }}>
              {comments.map(c => (
                <div key={c.commentId} style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', gap: 8, fontSize: 12 }}>
                    <div style={{ width: 24, height: 24, borderRadius: '50%', flexShrink: 0, background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 8, fontWeight: 800 }}>{(c.writerNickname || '?').slice(0, 2)}</div>
                    <div style={{ flex: 1 }}>
                      <span style={{ fontWeight: 600, marginRight: 6 }}>{c.writerNickname}</span>
                      <span style={{ color: t.text }}>{c.content}</span>
                      <div style={{ display: 'flex', gap: 10, marginTop: 4, fontSize: 11, color: t.textDim }}>
                        <button onClick={() => toggleCommentLike(c.commentId)} style={{ background: 'transparent', border: 'none', color: t.textDim, fontSize: 11, cursor: 'pointer', padding: 0 }}>♡ {c.likeCount || 0}</button>
                        {window.__connectfinAuthUser?.id && c.writerId && window.__connectfinAuthUser.id === c.writerId && (
                          <button onClick={() => deleteComment(c.commentId)} style={{ background: 'transparent', border: 'none', color: t.textDim, fontSize: 11, cursor: 'pointer', padding: 0 }}>삭제</button>
                        )}
                      </div>
                      {c.replyCount > 0 && !expandedReplies[c.commentId] && (
                        <button onClick={() => loadReplies(c.commentId)} style={{ display: 'block', background: 'transparent', border: 'none', color: t.accent, fontSize: 11, cursor: 'pointer', padding: 0, marginTop: 4 }}>답글 {c.replyCount}개 보기</button>
                      )}
                      {expandedReplies[c.commentId] && expandedReplies[c.commentId].map(r => (
                        <div key={r.commentId} style={{ display: 'flex', gap: 6, marginTop: 6, marginLeft: 16, fontSize: 11 }}>
                          <div style={{ width: 18, height: 18, borderRadius: '50%', flexShrink: 0, background: t.line, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 7, fontWeight: 700, color: t.textDim }}>{(r.writerNickname || '?').slice(0, 2)}</div>
                          <div><span style={{ fontWeight: 600, marginRight: 4 }}>{r.writerNickname}</span>{r.content}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <input type="text" value={commentInput} onChange={e => setCommentInput(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submitComment(); } }}
              placeholder="댓글 달기..." style={{ flex: 1, padding: '8px 12px', borderRadius: 8, border: `1px solid ${t.line}`, background: 'transparent', color: t.text, fontSize: 12, fontFamily: t.font }}/>
            <button onClick={submitComment} disabled={commentSubmitting || !commentInput.trim()} style={{
              padding: '6px 12px', borderRadius: 8, border: 'none', background: t.accent, color: '#fff', fontSize: 11, fontWeight: 600, cursor: 'pointer', opacity: commentSubmitting || !commentInput.trim() ? 0.5 : 1,
            }}>{commentSubmitting ? '...' : '게시'}</button>
          </div>
        </div>
      )}
    </div>
  );
}

function FanPostFullCard({ post, artist, t, theme, authUser, onDeleted }) {
  const [commentInput, setCommentInput] = React.useState('');
  const [commentSubmitting, setCommentSubmitting] = React.useState(false);
  const [comments, setComments] = React.useState([]);
  const [showComments, setShowComments] = React.useState(false);
  const [expandedReplies, setExpandedReplies] = React.useState({});

  const loadComments = () => {
    if (!post.artistId || !post.id) return;
    window.ConnectfinAPI.api(`/api/post/v1/artists/${post.artistId}/fan-posts/${post.id}`)
      .then(data => { if (data.comments?.content) setComments(data.comments.content); })
      .catch(err => { console.warn('Comments load failed:', err?.message || err); });
  };

  const submitComment = async () => {
    if (!commentInput.trim() || commentSubmitting) return;
    if (!window.ConnectfinAPI.getToken()) { alert('로그인이 필요합니다.'); return; }
    if (!post.artistId || !post.id) return;
    setCommentSubmitting(true);
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${post.artistId}/fan-posts/${post.id}/comments`,
        { method: 'POST', body: JSON.stringify({ content: commentInput.trim(), parentId: null }) }
      );
      setCommentInput('');
      loadComments();
    } catch (err) {
      alert('댓글 작성 실패: ' + (err.message || ''));
    } finally {
      setCommentSubmitting(false);
    }
  };

  const loadReplies = (commentId) => {
    window.ConnectfinAPI.api(`/api/post/v1/artists/${post.artistId}/fan-posts/${post.id}/comments/${commentId}/replies`)
      .then(data => {
        setExpandedReplies(prev => ({ ...prev, [commentId]: data.content || [] }));
      })
      .catch(err => { console.warn('Replies load failed:', err?.message || err); });
  };

  const deletePost = async () => {
    if (!confirm('이 게시글을 삭제하시겠습니까?')) return;
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${post.artistId}/fan-posts/${post.id}`,
        { method: 'DELETE' }
      );
      if (onDeleted) onDeleted(post.id);
    } catch (err) {
      alert('삭제 실패: ' + (err.message || ''));
    }
  };

  const deleteComment = async (commentId) => {
    if (!confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${post.artistId}/fan-posts/${post.id}/comments/${commentId}`,
        { method: 'DELETE' }
      );
      loadComments();
    } catch (err) {
      alert('삭제 실패: ' + (err.message || ''));
    }
  };

  const toggleCommentLike = async (commentId) => {
    try {
      await window.ConnectfinAPI.api(
        `/api/post/v1/artists/${post.artistId}/fan-posts/${post.id}/comments/${commentId}/likes/toggle`,
        { method: 'POST' }
      );
      loadComments();
    } catch (err) {
      console.warn('Comment like failed:', err?.message || err);
    }
  };

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
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 700, fontSize: 13 }}>
            {post.author}
            {post.fanMembershipSubscribed && <span style={{ color: t.accent2, marginLeft: 4 }}>✓</span>}
            {post.dmSubscribed && <span style={{ color: t.accent, marginLeft: 2 }}>✉</span>}
            {!post.fanMembershipSubscribed && !post.dmSubscribed && <span style={{ color: t.accent2 }}> ✓✓</span>}
          </div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>{post.timeAgo}</div>
        </div>
        {authUser?.id && post.writerId && authUser.id === post.writerId && (
          <button onClick={deletePost} style={{
            background: 'transparent', border: 'none', color: t.textDim, fontSize: 11, cursor: 'pointer', padding: 0,
          }}>삭제</button>
        )}
      </div>
      {post.tag && <div style={{ color: t.hot, fontSize: 13, fontWeight: 700, marginBottom: 4 }}>{post.tag}</div>}
      {post.hashtags && post.hashtags.length > 0 && (
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 4 }}>
          {post.hashtags.map(tag => (
            <span key={tag} style={{ color: t.accent, fontSize: 12, fontWeight: 600 }}>#{tag}</span>
          ))}
        </div>
      )}
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
      <div style={{ display: 'flex', gap: 16, marginTop: 10, fontSize: 12, color: t.textDim, alignItems: 'center' }}>
        {post.artistId && post.id ? (
          <LikeButton t={t} postType="FAN_POST" artistId={post.artistId} postId={post.id} initialCount={post.likeCount ?? 0}/>
        ) : (
          <span>♡ {post.likes}</span>
        )}
        {post.comments !== null && <span>💬 {post.comments}</span>}
      </div>
      {post.artistId && post.id && (
        <div style={{ marginTop: 12, borderTop: `1px solid ${t.line}`, paddingTop: 10 }}>
          {post.comments > 0 && !showComments && (
            <button onClick={() => { setShowComments(true); loadComments(); }} style={{
              background: 'transparent', border: 'none', color: t.textDim, fontSize: 12, cursor: 'pointer', padding: 0, marginBottom: 8,
            }}>💬 댓글 {post.comments}개 보기</button>
          )}
          {showComments && comments.length > 0 && (
            <div style={{ marginBottom: 10 }}>
              {comments.map(c => (
                <div key={c.commentId} style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', gap: 8, fontSize: 12 }}>
                    <div style={{ width: 24, height: 24, borderRadius: '50%', flexShrink: 0, background: `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 8, fontWeight: 800 }}>{(c.writerNickname || '?').slice(0, 2)}</div>
                    <div style={{ flex: 1 }}>
                      <span style={{ fontWeight: 600, marginRight: 6 }}>{c.writerNickname}</span>
                      <span style={{ color: t.text }}>{c.content}</span>
                      <div style={{ display: 'flex', gap: 10, marginTop: 4, fontSize: 11, color: t.textDim }}>
                        <button onClick={() => toggleCommentLike(c.commentId)} style={{ background: 'transparent', border: 'none', color: t.textDim, fontSize: 11, cursor: 'pointer', padding: 0 }}>♡ {c.likeCount || 0}</button>
                        {authUser?.id && c.writerId && authUser.id === c.writerId && (
                          <button onClick={() => deleteComment(c.commentId)} style={{ background: 'transparent', border: 'none', color: t.textDim, fontSize: 11, cursor: 'pointer', padding: 0 }}>삭제</button>
                        )}
                      </div>
                      {c.replyCount > 0 && !expandedReplies[c.commentId] && (
                        <button onClick={() => loadReplies(c.commentId)} style={{ display: 'block', background: 'transparent', border: 'none', color: t.accent, fontSize: 11, cursor: 'pointer', padding: 0, marginTop: 4 }}>답글 {c.replyCount}개 보기</button>
                      )}
                      {expandedReplies[c.commentId] && expandedReplies[c.commentId].map(r => (
                        <div key={r.commentId} style={{ display: 'flex', gap: 6, marginTop: 6, marginLeft: 16, fontSize: 11 }}>
                          <div style={{ width: 18, height: 18, borderRadius: '50%', flexShrink: 0, background: t.line, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 7, fontWeight: 700, color: t.textDim }}>{(r.writerNickname || '?').slice(0, 2)}</div>
                          <div><span style={{ fontWeight: 600, marginRight: 4 }}>{r.writerNickname}</span>{r.content}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <input type="text" value={commentInput} onChange={e => setCommentInput(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submitComment(); } }}
              placeholder="댓글 달기..." style={{ flex: 1, padding: '8px 12px', borderRadius: 8, border: `1px solid ${t.line}`, background: 'transparent', color: t.text, fontSize: 12, fontFamily: t.font }}/>
            <button onClick={submitComment} disabled={commentSubmitting || !commentInput.trim()} style={{
              padding: '6px 12px', borderRadius: 8, border: 'none', background: t.accent, color: '#fff', fontSize: 11, fontWeight: 600, cursor: 'pointer', opacity: commentSubmitting || !commentInput.trim() ? 0.5 : 1,
            }}>{commentSubmitting ? '...' : '게시'}</button>
          </div>
        </div>
      )}
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

// ────────── ADMIN (관리자 간이 패널) ──────────
function TabAdmin({ t, theme, artist }) {
  // ── 라이브 ──
  const [liveTitle, setLiveTitle] = React.useState('');
  const [liveCreating, setLiveCreating] = React.useState(false);
  const [lives, setLives] = React.useState([]);

  const loadLives = () => {
    window.ConnectfinAPI.api(`/api/v1/artists/${artist.id}/lives`)
      .then(data => setLives(Array.isArray(data) ? data.slice(0, 5) : []))
      .catch(err => { console.warn('Lives load failed:', err?.message || err); });
  };

  React.useEffect(() => { loadLives(); }, [artist.id]);

  const createLive = async () => {
    if (!liveTitle.trim() || liveCreating) return;
    setLiveCreating(true);
    try {
      await window.ConnectfinAPI.api(`/api/v1/admin/artists/${artist.id}/lives`, {
        method: 'POST', body: JSON.stringify({ title: liveTitle.trim(), description: '', thumbnailUrl: '' })
      });
      setLiveTitle('');
      loadLives();
      alert('라이브 생성 완료!');
    } catch (err) {
      alert('라이브 생성 실패: ' + (err.message || '권한이 없습니다'));
    } finally {
      setLiveCreating(false);
    }
  };

  const startLive = async (liveId) => {
    try {
      await window.ConnectfinAPI.api(`/api/v1/admin/artists/${artist.id}/lives/${liveId}/start`, { method: 'PATCH' });
      loadLives();
      alert('라이브 시작!');
    } catch (err) { alert('시작 실패: ' + (err.message || '')); }
  };

  const endLive = async (liveId) => {
    try {
      await window.ConnectfinAPI.api(`/api/v1/admin/artists/${artist.id}/lives/${liveId}/end`, { method: 'PATCH' });
      loadLives();
      alert('라이브 종료!');
    } catch (err) { alert('종료 실패: ' + (err.message || '')); }
  };

  // ── 래플 ──
  const [raffleTitle, setRaffleTitle] = React.useState('');
  const [raffleWinners, setRaffleWinners] = React.useState(3);
  const [raffleDuration, setRaffleDuration] = React.useState(30);
  const [raffleCreating, setRaffleCreating] = React.useState(false);
  const [raffles, setRaffles] = React.useState([]);

  const loadRaffles = () => {
    window.ConnectfinAPI.api(`/api/v1/artists/${artist.id}/raffles`)
      .then(data => {
        const list = Array.isArray(data) ? data : (data?.content || []);
        setRaffles(list.slice(0, 5));
      })
      .catch(err => { console.warn('Raffles load failed:', err?.message || err); });
  };

  React.useEffect(() => { loadRaffles(); }, [artist.id]);

  const createRaffle = async () => {
    if (!raffleTitle.trim() || raffleCreating) return;
    setRaffleCreating(true);
    try {
      await window.ConnectfinAPI.api(`/api/v1/admin/artists/${artist.id}/raffles`, {
        method: 'POST', body: JSON.stringify({
          title: raffleTitle.trim(),
          totalWinners: raffleWinners,
          durationMinutes: raffleDuration,
          entryCondition: 'ALL',
          rewardType: 'MEMBERSHIP_EXTENSION'
        })
      });
      setRaffleTitle('');
      loadRaffles();
      alert('래플 생성 완료!');
    } catch (err) {
      alert('래플 생성 실패: ' + (err.message || '권한이 없습니다'));
    } finally {
      setRaffleCreating(false);
    }
  };

  const startRaffle = async (raffleId) => {
    try {
      await window.ConnectfinAPI.api(`/api/v1/admin/artists/${artist.id}/raffles/${raffleId}/start`, { method: 'PATCH' });
      loadRaffles();
      alert('래플 시작!');
    } catch (err) { alert('시작 실패: ' + (err.message || '')); }
  };

  // ── DM 일괄 발송 ──
  const [dmContent, setDmContent] = React.useState('');
  const [dmSending, setDmSending] = React.useState(false);

  const broadcastDm = async () => {
    if (!dmContent.trim() || dmSending) return;
    if (!window.ConnectfinAPI.getToken()) { alert('로그인이 필요합니다.'); return; }
    setDmSending(true);
    try {
      // 이미 연결된 stomp 클라이언트가 있으면 connectStomp가 그대로 반환한다.
      // 미연결 상태면 새 연결을 활성화하지만 즉시 connected가 되지 않으므로 안내한다.
      const client = window.ConnectfinAPI.connectStomp(() => {});
      if (!client || !client.connected) {
        alert('STOMP 연결이 없습니다. 라이브 채팅 화면을 먼저 열어 연결해 주세요.');
        return;
      }
      // 백엔드 @MessageMapping("/dm/broadcast/{artistId}")는 @Payload String을 받으므로 plain text 전송
      client.publish({
        destination: `/pub/dm/broadcast/${artist.id}`,
        body: dmContent.trim(),
      });
      setDmContent('');
      alert('DM 일괄 발송 완료!');
    } catch (err) {
      alert('발송 실패: ' + (err.message || ''));
    } finally {
      setDmSending(false);
    }
  };

  // ── YouTube import ──
  const [youtubeUrl, setYoutubeUrl] = React.useState('');
  const [youtubeImporting, setYoutubeImporting] = React.useState(false);

  // ── 신규 아티스트 생성 ──
  const [newArtist, setNewArtist] = React.useState({
    name: '', slug: '', stageName: '', intro: '',
    profileImageUrl: '', coverImageUrl: '',
  });
  const [creatingArtist, setCreatingArtist] = React.useState(false);

  const createArtist = async () => {
    const a = newArtist;
    if (!a.name.trim() || !a.slug.trim() || !a.stageName.trim() || creatingArtist) return;
    setCreatingArtist(true);
    try {
      const res = await window.ConnectfinAPI.api('/api/member/v1/artists', {
        method: 'POST',
        body: JSON.stringify({
          name: a.name.trim(),
          slug: a.slug.trim(),
          stageName: a.stageName.trim(),
          profileImageUrl: a.profileImageUrl.trim() || 'https://cdn.connectfin.com/default-profile.jpg',
          coverImageUrl: a.coverImageUrl.trim() || 'https://cdn.connectfin.com/default-cover.jpg',
          intro: a.intro.trim() || '소개 준비중',
        }),
      });
      // 생성된 아티스트를 ARTISTS에 즉시 머지 + 갱신 알림
      const id = res?.id || res?.artistId;
      if (id && Array.isArray(window.ARTISTS)) {
        const hue = (id * 47) % 360;
        window.ARTISTS.push({
          id, name: a.name.trim(), slug: a.slug.trim(),
          stage: a.stageName.trim(), genre: 'NEW · ARTIST',
          color1: `hsl(${hue}, 70%, 60%)`, color2: `hsl(${hue}, 70%, 80%)`,
          members: 1, live: false, viewers: 0, followers: '0',
          profileImageUrl: a.profileImageUrl.trim(), fromBackend: true,
        });
        window.dispatchEvent(new CustomEvent('connectfin:artists-changed'));
      }
      // 또한 백엔드에서 다시 풀로드 (정규화된 데이터 확보)
      window.ConnectfinAPI.loadArtists();
      setNewArtist({ name: '', slug: '', stageName: '', intro: '', profileImageUrl: '', coverImageUrl: '' });
      alert(`아티스트 생성 완료! ID=${id}\n좌측 네비/검색에서 즉시 확인 가능`);
    } catch (err) {
      alert('생성 실패: ' + (err.message || ''));
    } finally {
      setCreatingArtist(false);
    }
  };

  const importYoutube = async () => {
    if (!youtubeUrl.trim() || youtubeImporting) return;
    setYoutubeImporting(true);
    try {
      await window.ConnectfinAPI.api(`/api/media/v1/artists/${artist.id}/youtube-videos`, {
        method: 'POST', body: JSON.stringify({ youtubeUrl: youtubeUrl.trim() })
      });
      setYoutubeUrl('');
      alert('YouTube 영상 등록 완료!');
    } catch (err) {
      alert('등록 실패: ' + (err.message || '권한이 없거나 중복된 영상입니다'));
    } finally {
      setYoutubeImporting(false);
    }
  };

  const inputStyle = {
    flex: 1, padding: '8px 12px', borderRadius: 8,
    border: `1px solid ${t.line}`, background: 'transparent',
    color: t.text, fontSize: 12, fontFamily: t.font,
  };
  const adminBtnStyle = (disabled) => ({
    padding: '6px 14px', borderRadius: 8, border: 'none',
    background: t.accent, color: '#fff', fontSize: 12, fontWeight: 600,
    cursor: 'pointer', opacity: disabled ? 0.5 : 1,
  });
  const sectionStyle = {
    padding: 16, marginBottom: 14, borderRadius: 12,
    border: `1px solid ${t.line}`,
    background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface,
  };

  return (
    <div>
      <div style={{ fontSize: 11, color: t.textDim, marginBottom: 16, fontFamily: t.fontMono }}>
        ARTIST / SUPER_ADMIN 권한 필요 · 권한 없으면 API가 403 반환
      </div>

      {/* 라이브 관리 */}
      <div style={sectionStyle}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 12 }}>라이브 관리</div>
        <div style={{ display: 'flex', gap: 6, marginBottom: 10 }}>
          <input value={liveTitle} onChange={e => setLiveTitle(e.target.value)}
            placeholder="라이브 제목" style={inputStyle}/>
          <button onClick={createLive} disabled={liveCreating || !liveTitle.trim()} style={adminBtnStyle(liveCreating || !liveTitle.trim())}>
            {liveCreating ? '...' : '생성'}
          </button>
        </div>
        {lives.map(live => {
          const status = live.liveStatus || live.status;
          return (
            <div key={live.liveId || live.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', fontSize: 12 }}>
              <span>{live.title} <span style={{ color: t.textDim, fontFamily: t.fontMono }}>({status})</span></span>
              <div style={{ display: 'flex', gap: 4 }}>
                {status === 'SCHEDULED' && (
                  <button onClick={() => startLive(live.liveId || live.id)} style={{ ...adminBtnStyle(false), padding: '3px 8px', fontSize: 11 }}>시작</button>
                )}
                {status === 'LIVE' && (
                  <button onClick={() => endLive(live.liveId || live.id)} style={{ ...adminBtnStyle(false), padding: '3px 8px', fontSize: 11, background: t.hot || '#E24B4A' }}>종료</button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* 래플 관리 */}
      <div style={sectionStyle}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 12 }}>래플 관리</div>
        <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
          <input value={raffleTitle} onChange={e => setRaffleTitle(e.target.value)}
            placeholder="래플 제목" style={inputStyle}/>
        </div>
        <div style={{ display: 'flex', gap: 6, marginBottom: 10, alignItems: 'center' }}>
          <label style={{ fontSize: 11, color: t.textDim }}>당첨</label>
          <input type="number" value={raffleWinners} onChange={e => setRaffleWinners(Number(e.target.value))} min={1}
            style={{ ...inputStyle, flex: 'none', width: 50, textAlign: 'center' }}/>
          <label style={{ fontSize: 11, color: t.textDim }}>명 ·</label>
          <label style={{ fontSize: 11, color: t.textDim }}>시간</label>
          <input type="number" value={raffleDuration} onChange={e => setRaffleDuration(Number(e.target.value))} min={1}
            style={{ ...inputStyle, flex: 'none', width: 50, textAlign: 'center' }}/>
          <label style={{ fontSize: 11, color: t.textDim }}>분</label>
          <button onClick={createRaffle} disabled={raffleCreating || !raffleTitle.trim()} style={adminBtnStyle(raffleCreating || !raffleTitle.trim())}>
            {raffleCreating ? '...' : '생성'}
          </button>
        </div>
        {raffles.map(r => (
          <div key={r.raffleId || r.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', fontSize: 12 }}>
            <span>{r.title} <span style={{ color: t.textDim, fontFamily: t.fontMono }}>({r.status})</span></span>
            {r.status === 'PENDING' && (
              <button onClick={() => startRaffle(r.raffleId || r.id)} style={{ ...adminBtnStyle(false), padding: '3px 8px', fontSize: 11 }}>시작</button>
            )}
          </div>
        ))}
      </div>

      {/* DM 일괄 발송 */}
      <div style={sectionStyle}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 12 }}>DM 일괄 발송</div>
        <div style={{ display: 'flex', gap: 6 }}>
          <input value={dmContent} onChange={e => setDmContent(e.target.value)}
            placeholder="구독자 전체에게 보낼 메시지" style={inputStyle}
            onKeyDown={e => { if (e.key === 'Enter') broadcastDm(); }}/>
          <button onClick={broadcastDm} disabled={dmSending || !dmContent.trim()} style={adminBtnStyle(dmSending || !dmContent.trim())}>
            {dmSending ? '...' : '발송'}
          </button>
        </div>
        <div style={{ fontSize: 10, color: t.textDim, marginTop: 6 }}>STOMP /pub/dm/broadcast/{artist.id} — 아티스트 멤버만 발송 가능</div>
      </div>

      {/* YouTube import */}
      <div style={sectionStyle}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 12 }}>YouTube 영상 등록</div>
        <div style={{ display: 'flex', gap: 6 }}>
          <input value={youtubeUrl} onChange={e => setYoutubeUrl(e.target.value)}
            placeholder="YouTube URL (https://youtube.com/watch?v=...)" style={inputStyle}
            onKeyDown={e => { if (e.key === 'Enter') importYoutube(); }}/>
          <button onClick={importYoutube} disabled={youtubeImporting || !youtubeUrl.trim()} style={adminBtnStyle(youtubeImporting || !youtubeUrl.trim())}>
            {youtubeImporting ? '...' : '등록'}
          </button>
        </div>
      </div>

      {/* ── 신규 아티스트 생성 ── */}
      <div style={{ padding: 16, background: t.surface2, borderRadius: 12, border: `1px solid ${t.line}`, marginTop: 14 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 4 }}>
          <div style={{ fontWeight: 800, fontSize: 14 }}>신규 아티스트 생성</div>
          <div style={{ fontSize: 10, fontFamily: t.fontMono, color: t.textMuted }}>POST /api/member/v1/artists</div>
        </div>
        <div style={{ fontSize: 11, color: t.textDim, marginBottom: 12 }}>
          생성 즉시 좌측 네비, 검색, 라이브 스피어에 반영됩니다 · SUPER_ADMIN 권한 필요
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, marginBottom: 6 }}>
          <input value={newArtist.name} onChange={e => setNewArtist(p => ({ ...p, name: e.target.value }))}
            placeholder="이름 (예: NEXUS9)" style={inputStyle}/>
          <input value={newArtist.slug} onChange={e => setNewArtist(p => ({ ...p, slug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '') }))}
            placeholder="슬러그 (예: nexus9, 영문/숫자/-)" style={{ ...inputStyle, fontFamily: t.fontMono }}/>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 6, marginBottom: 6 }}>
          <input value={newArtist.stageName} onChange={e => setNewArtist(p => ({ ...p, stageName: e.target.value }))}
            placeholder="활동명 (예: 넥서스나인)" style={inputStyle}/>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, marginBottom: 6 }}>
          <input value={newArtist.profileImageUrl} onChange={e => setNewArtist(p => ({ ...p, profileImageUrl: e.target.value }))}
            placeholder="프로필 이미지 URL (선택)" style={{ ...inputStyle, fontFamily: t.fontMono, fontSize: 11 }}/>
          <input value={newArtist.coverImageUrl} onChange={e => setNewArtist(p => ({ ...p, coverImageUrl: e.target.value }))}
            placeholder="커버 이미지 URL (선택)" style={{ ...inputStyle, fontFamily: t.fontMono, fontSize: 11 }}/>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <input value={newArtist.intro} onChange={e => setNewArtist(p => ({ ...p, intro: e.target.value }))}
            placeholder="소개 (선택)" style={inputStyle}/>
          <button
            onClick={createArtist}
            disabled={creatingArtist || !newArtist.name.trim() || !newArtist.slug.trim() || !newArtist.stageName.trim()}
            style={adminBtnStyle(creatingArtist || !newArtist.name.trim() || !newArtist.slug.trim() || !newArtist.stageName.trim())}>
            {creatingArtist ? '...' : '생성'}
          </button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  DesktopArtistProfile, ArtistCoverBanner,
});

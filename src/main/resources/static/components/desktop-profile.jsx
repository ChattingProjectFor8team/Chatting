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

  React.useEffect(() => {
    window.ConnectfinAPI.api(`/api/member/v2/artists/${artist.id}`)
      .then(data => setArtistDetail(data))
      .catch(() => { /* mock 유지 */ });
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

  return (
    <div>
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
      .catch(() => { /* mock 유지 */ });
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

  React.useEffect(() => {
    setLoading(true);
    window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-posts`)
      .then(data => {
        setPosts(data.content.map(mapFanPost));
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
      })
      .catch(() => { /* mock 유지 */ })
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
      .catch(() => {})
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
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 14 }}>
        <Chip t={t} active={activeTab === 'latest'} onClick={() => setActiveTab('latest')}>전체</Chip>
        <Chip t={t} active={activeTab === 'hot'} onClick={() => { setActiveTab('hot'); if (hotPosts.length === 0) loadHot(); }}><span>🔥</span> Hot</Chip>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {(activeTab === 'latest' ? posts : hotPosts).map(p => <FanPostFullCard key={p.id} post={p} artist={artist} t={t} theme={theme}/>)}
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
      .catch(() => { /* mock 유지 */ })
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
      .catch(() => {})
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
      .catch(() => {})
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
      .catch(() => { /* mock 유지 */ })
      .finally(() => setLoading(false));
  }, [artist.id]);

  const openDetail = (letter) => {
    setSelected(letter);
    // 상세 API 호출 (목록과 필드가 다름)
    if (window.ConnectfinAPI.getToken() && letter.id) {
      window.ConnectfinAPI.api(`/api/post/v1/artists/${artist.id}/fan-letters/${letter.id}`)
        .then(data => setSelectedDetail(data))
        .catch(() => setSelectedDetail(null));
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
            .catch(() => {})
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
  const mockReplays = LIVE_REPLAYS.filter(r => r.artistId === artist.id);
  const [replays, setReplays] = React.useState(mockReplays);

  React.useEffect(() => {
    window.ConnectfinAPI.api(`/api/v1/artists/${artist.id}/lives/vods`)
      .then(data => {
        if (data.content && data.content.length > 0) {
          setReplays(data.content.map(mapLiveReplay));
        }
      })
      .catch(() => { /* mock 유지 */ });
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
          <div style={{ fontWeight: 700, fontSize: 13 }}>
            {post.author}
            {post.fanMembershipSubscribed && <span style={{ color: t.accent2, marginLeft: 4 }}>✓</span>}
            {post.dmSubscribed && <span style={{ color: t.accent, marginLeft: 2 }}>✉</span>}
            {!post.fanMembershipSubscribed && !post.dmSubscribed && <span style={{ color: t.accent2 }}> ✓✓</span>}
          </div>
          <div style={{ fontSize: 11, color: t.textDim, fontFamily: t.fontMono }}>{post.timeAgo}</div>
        </div>
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

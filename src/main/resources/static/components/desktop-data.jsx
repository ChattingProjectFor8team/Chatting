// Connectfin — extended fixture data for desktop web screens

// Notices (공지사항)
const NOTICES = [
  { id: 1, artistId: 1, pinned: true, emoji: '🎉', title: '[LUMEN8 1st Anniversary Pop-up : 다시 설레는 시작] 루멘 팝업스토어 운영 안내', date: '2026.04.17' },
  { id: 2, artistId: 1, pinned: true, emoji: '🛍', title: '[LUMEN8 1st Anniversary Pop-up : 다시 설레는 시작] 루멘 팝업스토어 MD 상품 안내', date: '2026.04.16' },
  { id: 3, artistId: 1, pinned: false, emoji: '📅', title: '[LUMEN8 1st Anniversary Pop-up] 루멘 팝업스토어 사전 예약 안내', date: '2026.04.13' },
  { id: 4, artistId: 1, pinned: false, emoji: '🎧', title: '[Prism 스트리밍 인증 이벤트] 안내', date: '2026.04.10' },
  { id: 5, artistId: 1, pinned: false, emoji: '🏬', title: '[LUMEN8 1st Anniversary Pop-up : 다시 설레는 시작] 루멘 팝업스토어 오픈 안내', date: '2026.04.10' },
  { id: 6, artistId: 1, pinned: false, emoji: '📢', title: '[공지사항] 2026년 "LUMEN8" 2차 창작 가이드라인 안내 (04.09 업데이트)', date: '2026.04.09' },
  { id: 7, artistId: 1, pinned: false, emoji: '✨', title: '[안내] 얼터너티브 유니버스 프로젝트 Error 01_Allergy 공개', date: '2026.04.06' },
  { id: 8, artistId: 1, pinned: false, emoji: '🎬', title: '[안내] 얼터너티브 유니버스 프로젝트 : Error 01_Allergy 공개 안내', date: '2026.04.05' },
  { id: 9, artistId: 1, pinned: false, emoji: '💌', title: '[멤버십 이벤트] 월간 DM 추첨 결과 공지', date: '2026.03.28' },
  { id: 10, artistId: 1, pinned: false, emoji: '⚠️', title: '[점검 안내] 4/2 02:00 ~ 04:00 서비스 점검 예정', date: '2026.03.25' },
];

// Fan letters — rendered as texture cards (notepad/blackboard/hearts/etc)
const FAN_LETTERS = [
  { id: 1, artistId: 1, texture: 'notepad', author: '바다뱅배무', body: '루멘빙 월요일 고생했어요~\n드뎌 내일이면 4월 22일! 1주년!!\n루멘빙이 온지 벌써 1년이나 되었다니 시간 정말 빠르다\n그래도 차곡차곡 쌓인 추억이 하나둘씩 기억나기 시작하네요 1주년이라 생각하니까 그런가?\n아무튼 오늘도 안녕히 주무세요 루멘공주~\n사랑해요 ❤️❤️' },
  { id: 2, artistId: 1, texture: 'blackboard', author: 'LUMINARY_07', body: '2026년 4월 21일\n루멘빙 1주년 잡업 스토리[2026.4.22~29.] D-1\n\nERROR 01_Allergy 공개[2026.4.6.] D+15\nFROM HERE 콘서트[2026.3.27~29.] D+23\n씨미 풀시 등록[2026.3.16.] D+36\nBECOME 발매[2026.1.22.] D+89\n루멘빙 DM 오픈[2025.12.23.] D+118\nHuman Eclipse 공개[2025.10.20.] D+184' },
  { id: 3, artistId: 1, texture: 'hearts', author: '별빛모아', body: '새로운 한 주가 다가왔던 하루였\n어느새 끝나가고 있는 밤이네 YUNA~\n점점 루멘8의 1주년이 다가오니까\n매우 기대되고 뭐랄까 설레는 기분이\n들었는데 내일 루멘 노래방이 온다니\n매우 행복해서 싱글벙글해 하이🥰\n뭔가 3번 방송 이후로 계속 조용한것\n같아 방송 준비로 인해서 많이\n바쁘다고 생각이 드는데 부디\n무사히 준비 잘하길 바라고 1주년 \n그리고 팝업 기간 내내 잘 진행하길\n응원하고 있을께 파이팅😭\n그럼 남은 하루 잘 마무리 짓길\n바라고 내일의 루멘 방송보러 꼭 갈게\n같이 1주년 축하하는 시간을 가지자!\n오늘도 별빛들에게 행복만안\n소식을 전해주러 온 루멘만을\n세상에서 제일 많이 사랑해💙' },
  { id: 4, artistId: 1, texture: 'balloon', author: 'rainpeach', body: '루멘사랑해루멘사랑해루멘사랑해루멘사랑해' },
  { id: 5, artistId: 1, texture: 'notepad', author: 'drift_ko', body: '진짜 못할 말 쑥스럽게\n그래도 꼭 전하고 싶어서 써봐요\n YUNA 덕분에 나의 2026이\n훨씬 반짝여\n고마워요, 늘 건강하길' },
  { id: 6, artistId: 1, texture: 'grid', author: 'neonpop', body: '1주년 축하해요!!!\n앞으로의 모든 순간도\n응원할게요 🩷' },
];

// Highlights (상단 큰 가로 스크롤 카드)
const HIGHLIGHTS = [
  { id: 1, artistId: 1, title: 'FROM HERE 콘서트 현장', tag: 'CONCERT', emoji: '🎤' },
  { id: 2, artistId: 1, title: 'Prism M/V Teaser', tag: 'TEASER', emoji: '▶' },
  { id: 3, artistId: 1, title: '멤버 Vlog · YUNA편', tag: 'VLOG', emoji: '📹' },
  { id: 4, artistId: 1, title: '1주년 기념 팝업 현장', tag: 'EVENT', emoji: '🎉' },
  { id: 5, artistId: 1, title: '팬사인회 비하인드', tag: 'BEHIND', emoji: '✨' },
];

// LIVE Replays
const LIVE_REPLAYS = [
  { id: 1, artistId: 1, title: '루멘의 기습 노래방 (멤버십 전용)', duration: '40:53', date: '04.16. 22:47', plays: '8.9K', likes: '14.9K', comments: 60, voiceOnly: true, membership: false },
  { id: 2, artistId: 1, title: '루멘8 기습 노래방 (멤버십 전용)', duration: '18:02', date: '04.12. 18:02', plays: '12.1K', likes: '132K', comments: 114, voiceOnly: false, membership: true },
  { id: 3, artistId: 1, title: '1', duration: '15:30', date: '04.10. 15:30', plays: '3.9K', likes: '14.6K', comments: 38, voiceOnly: false, membership: true },
  { id: 4, artistId: 1, title: '콘서트가 끝이 났네유 🥰🫶', duration: '10:46', date: '03.29. 20:40', plays: '9.7K', likes: '26.6K', comments: 179, voiceOnly: true, membership: false },
  { id: 5, artistId: 1, title: '퇴근길 랜덤 스트림', duration: '1:03:09', date: '03.22. 19:12', plays: '18.2K', likes: '42K', comments: 321, voiceOnly: true, membership: false },
  { id: 6, artistId: 1, title: '연습실 몰래 방송', duration: '51:06', date: '03.18. 23:04', plays: '22.8K', likes: '58K', comments: 491, voiceOnly: true, membership: true },
  { id: 7, artistId: 1, title: 'Q&A 털어보기', duration: '28:40', date: '03.11. 20:00', plays: '14.2K', likes: '31K', comments: 208, voiceOnly: false, membership: false },
  { id: 8, artistId: 1, title: '비하인드 첫공개', duration: '12:11', date: '03.05. 21:15', plays: '6.5K', likes: '22K', comments: 142, voiceOnly: false, membership: true },
];

// Media items with category splits
const MEDIA_EXTENDED = [
  { id: 1, artistId: 1, title: '📸 FROM HERE 1st Fan Concert – Opening, 지금부터(Onward)...', date: '2026.04.05', duration: '05:15', isNew: true, membership: false, kind: 'video' },
  { id: 2, artistId: 1, title: '콘서트 안무 연습 비하인드 🤫', date: '2026.02.21', duration: '02:41', isNew: true, membership: false, kind: 'video' },
  { id: 3, artistId: 1, title: '[Error :warning:] ACCESS TEMPORARILY GRANTED', date: '2026.04.03', duration: '00:57', isNew: false, membership: false, kind: 'video' },
  { id: 4, artistId: 1, title: '루멘의 꿈 속 들여다보기', date: '2026.02.02', duration: '00:46', isNew: false, membership: true, kind: 'video' },
  { id: 5, artistId: 1, title: '멤버십 가입을 해주신 루멘이 여러분들께', date: '2026.01.27', duration: '00:35', isNew: false, membership: true, kind: 'video' },
  { id: 6, artistId: 1, title: 'LUMEN8 - Become Official MV', date: '2026.01.23', duration: '03:12', isNew: false, membership: false, kind: 'video' },
  { id: 7, artistId: 1, title: 'Prism - Behind Scene', date: '2025.12.30', duration: '08:22', isNew: false, membership: true, kind: 'video' },
  { id: 8, artistId: 1, title: '연말 인사 영상 💌', date: '2025.12.25', duration: '01:15', isNew: false, membership: false, kind: 'video' },
];

// Jelly packages (일반충전)
const JELLY_PACKS = [
  { jelly: 4, price: '₩1,200', bonus: 0, benefit: null },
  { jelly: 8, price: '₩2,400', bonus: 0, benefit: null },
  { jelly: 20, price: '₩6,000', bonus: 1, benefit: '5% 혜택' },
  { jelly: 40, price: '₩12,000', bonus: 3, benefit: '7% 혜택' },
  { jelly: 60, price: '₩18,000', bonus: 5, benefit: '8% 혜택' },
  { jelly: 80, price: '₩24,000', bonus: 7, benefit: '8% 혜택' },
  { jelly: 120, price: '₩36,000', bonus: 11, benefit: '9% 혜택' },
  { jelly: 160, price: '₩48,000', bonus: 15, benefit: '9% 혜택' },
  { jelly: 240, price: '₩72,000', bonus: 24, benefit: '10% 혜택', best: true },
];

// Home hero slides (캐러셀)
const HERO_SLIDES = [
  { id: 1, artist: 'LUMEN8', kind: 'MERCH', title: '개화(FLOWERING)\nOFFICIAL MERCH', body: '커넥트핀샵에서 만나보세요!', color1: '#FDE68A', color2: '#FBBF24', textDark: true },
  { id: 2, artist: 'NOIR7', kind: 'CONCERT', title: 'NOIR7\n1st CONCERT TOUR', body: "'INTO THE LIGHT : Our WISH' ENCORE IN SEOUL 공식 상품", color1: '#0F172A', color2: '#334155', textDark: false },
  { id: 3, artist: 'hanabi*', kind: 'ALBUM', title: 'hanabi*\n2nd MINI ALBUM', body: "'FIREWORKS' pre-order now", color1: '#FCA5A5', color2: '#A78BFA', textDark: false },
];

// On LIVE (홈 하단)
const ON_LIVE_NOW = [
  { id: 1, artistId: 1, title: 'HBD 2 me', host: 'YUNA', group: 'LUMEN8', status: 'LIVE', kind: 'video' },
  { id: 2, artistId: 4, title: 'We on Fire Official Listening Party', host: 'hanabi*', group: 'hanabi*', status: 'Party', kind: 'voice' },
];

// Fan posts (Fan 탭)
const FAN_POSTS = [
  { id: 1, artistId: 1, author: '헤이즐넛초코', tag: '#Prism_Streaming', body: '20260421 유튜브뮤직', likes: '4.2K', comments: 238, timeAgo: '04. 21. 00:36', hasGrid: true, gridCount: 4 },
  { id: 2, artistId: 1, author: '헤이즐넛초코', tag: '#Prism_Streaming', body: '20260421 스포티파이', likes: '5.1K', comments: 288, timeAgo: '04. 21. 00:35', hasGrid: true, gridCount: 4 },
  { id: 3, artistId: 1, author: 'AspCY', body: '루멘시 ❤️ | 이 | | 식 사랑해!!! 악\n(+6)\n항상 너무 고마워!! 나의 우상!!!', likes: 20, comments: null, timeAgo: '5h' },
  { id: 4, artistId: 1, author: '누군가가웃는다면만족하는뱀뱀', body: '오늘이 중간고사 시험날이구나... 화이팅\n해야겠다.\n행복 부적 들고가야지...', likes: 35, comments: 16, timeAgo: '6h' },
  { id: 5, artistId: 1, author: '이선지', body: '오늘 국어 수행평가로 좋아하는 노래 또는 시 갈래로 나타내어 표현법 쓰기 시험 봤는데 다른 애들은 다 어려워 하던데 저는 왠지모르게 쉬워서 이틀만 준비...', likes: 24, comments: 6, timeAgo: '8h' },
  { id: 6, artistId: 1, author: '날이날', body: '22일날에 팝업예약 안하고 가면 굿즈 못사죠? 그죠?', likes: 17, comments: 5, timeAgo: '9h' },
  { id: 7, artistId: 1, author: 'Wilt', body: '[ 루멘 콘서트와 팝업을 못 갔으므로, 지금부터 굿즈 자체제작 시작하려합니다 ]', likes: 26, comments: 12, timeAgo: '11h' },
  { id: 8, artistId: 1, author: 'Legend', body: '루멘시', likes: 18, comments: null, timeAgo: '12h' },
];

// Artist posts (Artist 탭)
const ARTIST_POSTS = [
  { id: 1, artistId: 1, author: 'YUNA', body: '방송 킬까나😊', likes: '1.3K', comments: 641, time: '04. 08. 19:58', moment: true },
  { id: 2, artistId: 1, author: 'YUNA', body: '오늘.. 첫 프로젝트의 시작을 알리는 에피소드 1화 알레르기가 오후 6시에 공개 될 예정이에요!\n진짜 애니메이션도 너무너무 멋지고... 노래도 넘 좋으니까 다들 많이 관심 가져주셨으면 좋겠네요 하핫\n오늘은 방송 7시에 킬 예정입니다\n녹음 비하인드 썰이나 노래 관련 이야기 많이 해용\n이따 봐요😙', likes: '1.6K', comments: 443, time: '04. 06. 10:30' },
  { id: 3, artistId: 1, author: 'YUNA', body: '방송 온!!!! ⭐⭐⭐', likes: '1.3K', comments: 339, time: '04. 03. 17:47' },
  { id: 4, artistId: 1, author: 'YUNA', body: '보구치!!', likes: '980', comments: 218, time: '04. 02. 18:25' },
];

// ═══════════════════════════════════════════════════════════
// 나머지 5명 아티스트 데이터 (id 2~6)
// Kagerō(2), NOIR7(3), hanabi*(4), Velvet Static(5), ORBITAL(6)
// ═══════════════════════════════════════════════════════════

NOTICES.push(
  // Kagerō (J-ROCK)
  { id: 101, artistId: 2, pinned: true, emoji: '🎸', title: '[Kagerō] 4th Single "Mirage" 발매 안내', date: '2026.04.18' },
  { id: 102, artistId: 2, pinned: true, emoji: '🎤', title: '[TOUR] Kagerō LIVE "SHINKAI 2026" 티켓 예매 공지', date: '2026.04.12' },
  { id: 103, artistId: 2, pinned: false, emoji: '📻', title: '[RADIO] J-WAVE 심야방송 게스트 출연 안내', date: '2026.04.08' },
  { id: 104, artistId: 2, pinned: false, emoji: '💿', title: '[한정반] 블루레이 선착순 예약 판매', date: '2026.04.02' },
  { id: 105, artistId: 2, pinned: false, emoji: '📢', title: '[공지] 2차 창작 가이드라인 업데이트 (04.01)', date: '2026.04.01' },

  // NOIR7 (K-POP 보이그룹)
  { id: 201, artistId: 3, pinned: true, emoji: '🌟', title: "[NOIR7] 1st CONCERT TOUR 'INTO THE LIGHT' ENCORE IN SEOUL 공지", date: '2026.04.19' },
  { id: 202, artistId: 3, pinned: true, emoji: '🛍', title: '[NOIR7] ENCORE 공식 MD 상품 안내 (4/25 오픈)', date: '2026.04.17' },
  { id: 203, artistId: 3, pinned: false, emoji: '📸', title: '[NOIR7] 4th Mini Album "OUR WISH" 콘셉트 포토 공개', date: '2026.04.14' },
  { id: 204, artistId: 3, pinned: false, emoji: '🎬', title: '[NOIR7] 자체 콘텐츠 "SEVEN-TAKE" S2 런칭 안내', date: '2026.04.10' },
  { id: 205, artistId: 3, pinned: false, emoji: '💎', title: '[FANCLUB] SEVENTS 3기 모집 안내', date: '2026.04.05' },

  // hanabi* (우타이테)
  { id: 301, artistId: 4, pinned: true, emoji: '🎆', title: '[hanabi*] 2nd Mini Album "FIREWORKS" 선주문 오픈', date: '2026.04.20' },
  { id: 302, artistId: 4, pinned: false, emoji: '🎙', title: '[hanabi*] 첫 단독 콘서트 "BLOOM" 공지', date: '2026.04.15' },
  { id: 303, artistId: 4, pinned: false, emoji: '🎨', title: '[콜라보] 유명 일러스트레이터와의 아트북 크라우드펀딩', date: '2026.04.08' },
  { id: 304, artistId: 4, pinned: false, emoji: '📅', title: '[스케줄] 4월 방송 일정 안내', date: '2026.04.01' },

  // Velvet Static (인디록)
  { id: 401, artistId: 5, pinned: true, emoji: '🎸', title: '[Velvet Static] EP "Ashlight" 발매 공지', date: '2026.04.16' },
  { id: 402, artistId: 5, pinned: false, emoji: '🎤', title: '[공연] 홍대 롤링홀 단독 공연 오픈 안내', date: '2026.04.10' },
  { id: 403, artistId: 5, pinned: false, emoji: '📢', title: '[공지] 머치 스토어 정기 점검 안내', date: '2026.04.05' },

  // ORBITAL (힙합)
  { id: 501, artistId: 6, pinned: true, emoji: '🔥', title: '[ORBITAL] 크루 정규 1집 "GRAVITY" 트랙리스트 공개', date: '2026.04.19' },
  { id: 502, artistId: 6, pinned: true, emoji: '🎤', title: '[CYPHER] ORBITAL x OVERDRIVE 콜라보 사이퍼 공지', date: '2026.04.13' },
  { id: 503, artistId: 6, pinned: false, emoji: '🎬', title: '[MV] "Blacklight" 뮤직비디오 티저 공개', date: '2026.04.07' },
  { id: 504, artistId: 6, pinned: false, emoji: '📅', title: '[투어] 전국 클럽 투어 "NO CEILING" 라인업 공지', date: '2026.04.01' },
);

FAN_LETTERS.push(
  // Kagerō
  { id: 201, artistId: 2, texture: 'blackboard', author: 'hotaru_jp', body: '東京の夜更け、\nまだ音が残っている。\n\nKagerōのライブで感じた\nあの熱を胸に、\n今日も生きていく。\n\nありがとう。' },
  { id: 202, artistId: 2, texture: 'notepad', author: '深海星', body: '카게로 진짜 고마워요\n힘들 때마다 MIRAGE 반복 재생\n이번 투어도 건강히 돌아와줘요' },
  { id: 203, artistId: 2, texture: 'grid', author: 'riku.k', body: '새 싱글 미리 듣기만 했는데\n벌써 울어버렸어요\n이 감정을 음악으로 만들어줘서 고마워요' },
  { id: 204, artistId: 2, texture: 'hearts', author: 'fade_blue', body: '카게로의 소리는\n항상 나를 살렸어요\n계속 들려주세요 🌊' },

  // NOIR7
  { id: 301, artistId: 3, texture: 'balloon', author: 'SEVENTIE', body: 'NOIR7 ENCORE 진짜 기다렸어\n세븐스들 준비됐어!' },
  { id: 302, artistId: 3, texture: 'notepad', author: '세븐♡모아', body: '카이야 요즘 잠 잘 자?\n너무 바쁜 것 같아서 걱정돼\n팬미팅에서 봤을 때 많이 피곤해보였어\n건강 꼭 챙겨줘' },
  { id: 303, artistId: 3, texture: 'hearts', author: 'light_07', body: 'INTO THE LIGHT\n우리가 너희의 빛이 되어줄게\n사랑해 🤍' },
  { id: 304, artistId: 3, texture: 'grid', author: '세븐다이어리', body: '데뷔 1500일 축하해\n앞으로의 1500일도 함께' },
  { id: 305, artistId: 3, texture: 'blackboard', author: 'N7archive', body: '[ENCORE D-5]\n2026.04.26 SAT\n좌석 E7\n모든 순간을 기억할게.' },

  // hanabi*
  { id: 401, artistId: 4, texture: 'balloon', author: '小さな火', body: '하나비 짱 오늘도\n너무 예뻐요 🎆' },
  { id: 402, artistId: 4, texture: 'hearts', author: '꽃불', body: 'FIREWORKS 프리오더 성공!\n첫 콘서트도 너무 기대돼\n준비 많이 하고 있는거 다 알아\n우리도 열심히 응원할게요' },
  { id: 403, artistId: 4, texture: 'notepad', author: 'sparkle_9', body: '하나비의 목소리를 들으면\n마음이 따뜻해져요\n계속 노래해주세요' },

  // Velvet Static
  { id: 501, artistId: 5, texture: 'blackboard', author: 'ashlight.', body: '롤링홀 공연 일주일 앞으로\n이번엔 꼭 갈게요\n드럼 스틱 던져주세요 농담 🥁' },
  { id: 502, artistId: 5, texture: 'grid', author: '벨벳팬', body: '인디씬에 이런 밴드가 있다는 게\n우리 세대의 행운이에요\n오래 해주세요' },
  { id: 503, artistId: 5, texture: 'hearts', author: 'staticwave', body: '가사 하나하나가 시예요\n앨범 나오면 꼭 피지컬 살거예요' },

  // ORBITAL
  { id: 601, artistId: 6, texture: 'blackboard', author: 'lowgrav', body: 'GRAVITY 트랙리스트 보고\n기절함. 3, 7, 11번 제목만 봐도\n각이다. 발매일만 기다림.' },
  { id: 602, artistId: 6, texture: 'grid', author: 'orbit_crew', body: '사이퍼 영상 100번 돌려봄\n한국힙합 다시 살아났음' },
  { id: 603, artistId: 6, texture: 'notepad', author: 'rap_diary', body: '오늘도 출근길에 ORBITAL\n이 바이브 없으면 못 버텨요\n정규 1집 기다릴게요' },
);

HIGHLIGHTS.push(
  // Kagerō
  { id: 201, artistId: 2, title: 'SHINKAI TOUR 리허설', tag: 'REHEARSAL', emoji: '🎸' },
  { id: 202, artistId: 2, title: 'Mirage MV Behind', tag: 'BEHIND', emoji: '🎬' },
  { id: 203, artistId: 2, title: '새 싱글 스튜디오 세션', tag: 'STUDIO', emoji: '🎙' },
  { id: 204, artistId: 2, title: '라이브 셋리스트 공개', tag: 'SETLIST', emoji: '📝' },

  // NOIR7
  { id: 301, artistId: 3, title: 'INTO THE LIGHT 서울 앙코르', tag: 'CONCERT', emoji: '🌟' },
  { id: 302, artistId: 3, title: 'OUR WISH 컨셉포토', tag: 'PHOTO', emoji: '📸' },
  { id: 303, artistId: 3, title: 'SEVEN-TAKE EP.01', tag: 'CONTENT', emoji: '🎬' },
  { id: 304, artistId: 3, title: '멤버 VLOG · KAI', tag: 'VLOG', emoji: '📹' },
  { id: 305, artistId: 3, title: '팬사인회 현장', tag: 'EVENT', emoji: '✨' },

  // hanabi*
  { id: 401, artistId: 4, title: 'FIREWORKS 티저', tag: 'TEASER', emoji: '🎆' },
  { id: 402, artistId: 4, title: '단독 콘서트 연습실', tag: 'REHEARSAL', emoji: '🎙' },
  { id: 403, artistId: 4, title: '아트북 일러스트 공개', tag: 'ART', emoji: '🎨' },

  // Velvet Static
  { id: 501, artistId: 5, title: 'Ashlight EP 쇼케이스', tag: 'SHOWCASE', emoji: '🎸' },
  { id: 502, artistId: 5, title: '롤링홀 리허설', tag: 'REHEARSAL', emoji: '🥁' },

  // ORBITAL
  { id: 601, artistId: 6, title: 'CYPHER 영상', tag: 'CYPHER', emoji: '🔥' },
  { id: 602, artistId: 6, title: 'Blacklight MV 티저', tag: 'TEASER', emoji: '🎬' },
  { id: 603, artistId: 6, title: 'NO CEILING 투어 일정', tag: 'TOUR', emoji: '🗺' },
);

LIVE_REPLAYS.push(
  // Kagerō
  { id: 201, artistId: 2, title: 'Mirage 레코딩 세션 비하인드', duration: '45:12', date: '04.18. 21:30', plays: '14.2K', likes: '38K', comments: 210, voiceOnly: false, membership: false },
  { id: 202, artistId: 2, title: '심야 톡톡 · 깊은 밤의 이야기', duration: '1:12:45', date: '04.10. 23:00', plays: '22.1K', likes: '51K', comments: 388, voiceOnly: true, membership: false },
  { id: 203, artistId: 2, title: 'SHINKAI 투어 리허설 엿보기', duration: '32:04', date: '04.05. 18:40', plays: '18.8K', likes: '42K', comments: 254, voiceOnly: false, membership: true },
  { id: 204, artistId: 2, title: '어쿠스틱 라이브', duration: '28:16', date: '03.25. 20:00', plays: '9.4K', likes: '26K', comments: 142, voiceOnly: false, membership: false },

  // NOIR7
  { id: 301, artistId: 3, title: 'ENCORE 연습 현장 (멤버십 전용)', duration: '52:40', date: '04.19. 19:15', plays: '45.2K', likes: '188K', comments: 921, voiceOnly: false, membership: true },
  { id: 302, artistId: 3, title: '카이의 야식 타임 🍜', duration: '41:22', date: '04.16. 23:40', plays: '38.1K', likes: '142K', comments: 684, voiceOnly: true, membership: false },
  { id: 303, artistId: 3, title: 'SEVEN-TAKE 촬영 비하인드', duration: '18:55', date: '04.12. 17:20', plays: '22.4K', likes: '78K', comments: 412, voiceOnly: false, membership: false },
  { id: 304, artistId: 3, title: '세븐츠 사랑해 💜', duration: '08:11', date: '04.08. 22:00', plays: '29.8K', likes: '98K', comments: 512, voiceOnly: true, membership: false },
  { id: 305, artistId: 3, title: '데뷔 1500일 기념 방송', duration: '1:22:18', date: '04.01. 20:00', plays: '62.3K', likes: '210K', comments: 1184, voiceOnly: false, membership: false },

  // hanabi*
  { id: 401, artistId: 4, title: 'FIREWORKS 첫 번째 티저 공개', duration: '22:08', date: '04.20. 20:10', plays: '18.2K', likes: '42K', comments: 298, voiceOnly: false, membership: false },
  { id: 402, artistId: 4, title: '딩동! 스페셜 커버곡 라이브', duration: '34:45', date: '04.14. 21:30', plays: '12.6K', likes: '28K', comments: 167, voiceOnly: true, membership: false },
  { id: 403, artistId: 4, title: '콘서트 연습실 live', duration: '55:12', date: '04.05. 19:00', plays: '8.9K', likes: '19K', comments: 124, voiceOnly: false, membership: true },

  // Velvet Static
  { id: 501, artistId: 5, title: 'Ashlight 발매 전 첫 공개', duration: '18:40', date: '04.14. 22:00', plays: '5.4K', likes: '14K', comments: 88, voiceOnly: false, membership: false },
  { id: 502, artistId: 5, title: '작업실 일상', duration: '41:18', date: '04.03. 23:15', plays: '3.8K', likes: '9.2K', comments: 52, voiceOnly: true, membership: false },

  // ORBITAL
  { id: 601, artistId: 6, title: 'GRAVITY 트랙 미리듣기 세션', duration: '38:22', date: '04.19. 22:30', plays: '24.1K', likes: '62K', comments: 441, voiceOnly: false, membership: true },
  { id: 602, artistId: 6, title: 'Blacklight 레코딩 비하인드', duration: '25:04', date: '04.10. 20:00', plays: '19.2K', likes: '48K', comments: 312, voiceOnly: false, membership: false },
  { id: 603, artistId: 6, title: '크루원 랜덤 Q&A', duration: '1:02:18', date: '04.02. 21:40', plays: '31.8K', likes: '82K', comments: 598, voiceOnly: true, membership: false },
);

MEDIA_EXTENDED.push(
  // Kagerō
  { id: 201, artistId: 2, title: 'Mirage Official MV', date: '2026.04.18', duration: '04:28', isNew: true, membership: false, kind: 'video' },
  { id: 202, artistId: 2, title: 'Mirage Recording Session', date: '2026.04.12', duration: '08:14', isNew: true, membership: false, kind: 'video' },
  { id: 203, artistId: 2, title: 'SHINKAI Tour Teaser', date: '2026.04.06', duration: '01:18', isNew: false, membership: false, kind: 'video' },
  { id: 204, artistId: 2, title: '멤버 인터뷰 · Drummer Ren', date: '2026.03.28', duration: '12:42', isNew: false, membership: true, kind: 'video' },
  { id: 205, artistId: 2, title: '라이브 하우스 단독 세션', date: '2026.03.20', duration: '22:15', isNew: false, membership: true, kind: 'video' },
  { id: 206, artistId: 2, title: 'Acoustic Session · 終夜', date: '2026.03.10', duration: '05:33', isNew: false, membership: false, kind: 'video' },

  // NOIR7
  { id: 301, artistId: 3, title: "OUR WISH Concept Film", date: '2026.04.20', duration: '02:48', isNew: true, membership: false, kind: 'video' },
  { id: 302, artistId: 3, title: "ENCORE IN SEOUL 리허설 Vlog", date: '2026.04.18', duration: '14:22', isNew: true, membership: true, kind: 'video' },
  { id: 303, artistId: 3, title: "SEVEN-TAKE S2 EP.01", date: '2026.04.15', duration: '18:40', isNew: true, membership: false, kind: 'video' },
  { id: 304, artistId: 3, title: "KAI's Practice Room", date: '2026.04.10', duration: '08:12', isNew: false, membership: true, kind: 'video' },
  { id: 305, artistId: 3, title: 'Dance Practice · OUR WISH', date: '2026.04.05', duration: '03:54', isNew: false, membership: false, kind: 'video' },
  { id: 306, artistId: 3, title: '팬미팅 비하인드 🎤', date: '2026.03.30', duration: '11:20', isNew: false, membership: true, kind: 'video' },

  // hanabi*
  { id: 401, artistId: 4, title: 'FIREWORKS Teaser #01', date: '2026.04.20', duration: '00:58', isNew: true, membership: false, kind: 'video' },
  { id: 402, artistId: 4, title: 'BLOOM Concert Trailer', date: '2026.04.14', duration: '01:32', isNew: true, membership: false, kind: 'video' },
  { id: 403, artistId: 4, title: '커버곡 · 딩동 (Acoustic)', date: '2026.04.08', duration: '04:12', isNew: false, membership: false, kind: 'video' },
  { id: 404, artistId: 4, title: '스튜디오 풀영상', date: '2026.04.01', duration: '15:08', isNew: false, membership: true, kind: 'video' },

  // Velvet Static
  { id: 501, artistId: 5, title: 'Ashlight M/V', date: '2026.04.16', duration: '04:02', isNew: true, membership: false, kind: 'video' },
  { id: 502, artistId: 5, title: '롤링홀 쇼케이스 티저', date: '2026.04.08', duration: '01:15', isNew: false, membership: false, kind: 'video' },
  { id: 503, artistId: 5, title: 'Live Session · Night Drive', date: '2026.03.28', duration: '06:44', isNew: false, membership: true, kind: 'video' },

  // ORBITAL
  { id: 601, artistId: 6, title: 'Blacklight Official Teaser', date: '2026.04.18', duration: '00:48', isNew: true, membership: false, kind: 'video' },
  { id: 602, artistId: 6, title: 'ORBITAL x OVERDRIVE CYPHER', date: '2026.04.13', duration: '07:22', isNew: true, membership: false, kind: 'video' },
  { id: 603, artistId: 6, title: 'GRAVITY Track Preview', date: '2026.04.09', duration: '02:14', isNew: false, membership: true, kind: 'video' },
  { id: 604, artistId: 6, title: '크루 스튜디오 데일리', date: '2026.04.02', duration: '11:08', isNew: false, membership: true, kind: 'video' },
);

FAN_POSTS.push(
  // Kagerō
  { id: 201, artistId: 2, author: '深海ノート', tag: '#Mirage_Release', body: 'Mirage 수록곡 전부 다 명작인데\n특히 3번 트랙 "終夜"는 진짜 지리는 수준\n이 밴드는 매번 스스로를 뛰어넘는다', likes: 422, comments: 58, timeAgo: '2h', hasGrid: false },
  { id: 202, artistId: 2, author: 'blue_fade', body: '카게로 보컬 음색은 대체 불가능함\n저음부에서의 그 미묘한 떨림이\n곡 전체 분위기를 만든다', likes: 312, comments: 41, timeAgo: '4h' },
  { id: 203, artistId: 2, author: '螢火', tag: '#SHINKAI_Tour', body: 'SHINKAI 투어 나고야 공 진짜 미쳤음\n앙코르에서 운 사람 손', likes: 512, comments: 128, timeAgo: '6h' },
  { id: 204, artistId: 2, author: 'riku.k', body: '새 앨범 프리오더\n아직 안 한 사람 있어?', likes: 88, comments: 22, timeAgo: '8h' },

  // NOIR7
  { id: 301, artistId: 3, author: 'SEVEN♡SEVEN', tag: '#INTO_THE_LIGHT', body: '앙코르 서울 티켓팅 D-1\n심장이 멎을 것 같다\n이번엔 꼭 스탠딩 잡는다', likes: 2_481, comments: 312, timeAgo: '1h', hasGrid: true, gridCount: 4 },
  { id: 302, artistId: 3, author: '세븐모음', body: '카이 요즘 너무 멋있어서\n심장이 남아나질 않아\n세븐츠들 다 같은 마음이지?', likes: 3_102, comments: 482, timeAgo: '3h' },
  { id: 303, artistId: 3, author: 'light_07', tag: '#OUR_WISH', body: 'OUR WISH 컨셉포토 보고 기절함\n이번 콘셉트 진짜 미쳤음\n4월 25일만 기다린다', likes: 1_882, comments: 214, timeAgo: '5h', hasGrid: true, gridCount: 4 },
  { id: 304, artistId: 3, author: 'N7archive', body: '데뷔 1500일 타임라인\n정리해봤습니다\n처음엔 몰랐던 애들이\n지금은 인생의 반 이상이 되어있네요', likes: 2_201, comments: 398, timeAgo: '7h' },

  // hanabi*
  { id: 401, artistId: 4, author: '작은불꽃', tag: '#FIREWORKS_PREORDER', body: 'FIREWORKS 프리오더 오픈!!\n한정반 꼭 사야되는데\n카드 한도 충전하러 갑니다 🔥', likes: 614, comments: 92, timeAgo: '30m' },
  { id: 402, artistId: 4, author: 'hanabi_daily', body: '하나비 목소리는\n여름밤 불꽃 같아\n터지고 사라지지만\n오래 남는', likes: 422, comments: 58, timeAgo: '2h' },
  { id: 403, artistId: 4, author: 'sparkle_9', body: 'BLOOM 콘서트\n첫 단콘인데 얼마나 떨릴까\n우리가 응원 제일 세게 할게요', likes: 388, comments: 44, timeAgo: '5h' },

  // Velvet Static
  { id: 501, artistId: 5, author: 'ashlight.', tag: '#Ashlight_EP', body: 'Ashlight EP\n전곡 가사 다 외웠습니다\n특히 "재 되는 밤" 후렴 진짜 미친 곡', likes: 182, comments: 34, timeAgo: '3h' },
  { id: 502, artistId: 5, author: 'staticwave', body: '롤링홀 공연 티켓 잡았다\n드디어 처음으로 본다\n떨려서 잠이 안 옴', likes: 98, comments: 18, timeAgo: '6h' },

  // ORBITAL
  { id: 601, artistId: 6, author: 'lowgrav', tag: '#GRAVITY', body: 'GRAVITY 트랙리스트 보고\n바로 앨범 예판\n3, 7, 11번 각 미쳤음', likes: 1_022, comments: 214, timeAgo: '1h' },
  { id: 602, artistId: 6, author: 'rap_diary', tag: '#CYPHER', body: 'ORBITAL x OVERDRIVE 사이퍼\n한국힙합 씬 다시 살림\n이번 콜라보 레전드', likes: 1_412, comments: 288, timeAgo: '4h' },
  { id: 603, artistId: 6, author: 'orbit_crew', body: 'NO CEILING 투어 라인업 봤는데\n지방 공연도 많아서 좋음\n부산 공연에서 봅시다', likes: 488, comments: 82, timeAgo: '7h' },
);

ARTIST_POSTS.push(
  // Kagerō
  { id: 201, artistId: 2, author: 'REI', body: 'Mirage、\n迷いながら書いた。\n聴いてくれてありがとう。', likes: '2.1K', comments: 482, time: '04. 18. 20:00' },
  { id: 202, artistId: 2, author: 'REN', body: 'ドラム、\n指の皮が剥けた。\nでも楽しい。\n投ー、またね。', likes: '1.4K', comments: 212, time: '04. 14. 23:45' },
  { id: 203, artistId: 2, author: 'REI', body: '투어 시작 전\n팬 여러분에게 한 마디\n"너무 긴장하지 말고, 우리도 즐길게요"', likes: '1.8K', comments: 344, time: '04. 10. 18:22' },

  // NOIR7
  { id: 301, artistId: 3, author: 'KAI', body: 'ENCORE 준비 중...\n이번엔 진짜 다를 거야\n세븐츠 기대해도 좋아 💜', likes: '5.2K', comments: 892, time: '04. 19. 21:30' },
  { id: 302, artistId: 3, author: 'JUNO', body: '연습실. 오늘도 늦게까지.\n데뷔 1500일이 지났지만\n여전히 처음처럼 떨린다.', likes: '4.8K', comments: 712, time: '04. 17. 22:40' },
  { id: 303, artistId: 3, author: 'KAI', body: 'OUR WISH 컨셉포토\n다들 마음에 들었어?\n나는 이번 분위기가 제일 좋아.', likes: '6.1K', comments: 1024, time: '04. 14. 18:00' },
  { id: 304, artistId: 3, author: 'HARU', body: '팬미팅 영상 다시 돌려봤어\n진짜 너무 소중한 시간이었다\n또 만들자 우리', likes: '3.9K', comments: 588, time: '04. 10. 20:15' },

  // hanabi*
  { id: 401, artistId: 4, author: 'hanabi*', body: 'FIREWORKS 프리오더 시작했어요!\n처음 해보는 2nd mini인데\n진짜 한 곡 한 곡 애정 담아 만들었어요\n한정반 꼭 보세요 🎆', likes: '1.8K', comments: 412, time: '04. 20. 19:00' },
  { id: 402, artistId: 4, author: 'hanabi*', body: '오늘은 커버곡 녹음!\n힌트는... 여름! 밤! 불꽃!\n뭐일까요~?', likes: '1.2K', comments: 288, time: '04. 14. 21:30' },

  // Velvet Static
  { id: 501, artistId: 5, author: 'SEUNG', body: 'Ashlight EP 나왔습니다\n6곡 전부 다 직접 쓰고 녹음했어요\n꼭 헤드폰으로 들어주세요', likes: '512', comments: 88, time: '04. 16. 20:00' },
  { id: 502, artistId: 5, author: 'DRUMMER_Jin', body: '롤링홀 공연 연습 중\n스틱 2개 부러짐\n그만큼 열심히 하고 있다는 뜻', likes: '288', comments: 44, time: '04. 12. 23:10' },

  // ORBITAL
  { id: 601, artistId: 6, author: 'LOW-G', body: 'GRAVITY 트랙리스트 공개\n이번 앨범은 진짜 모든 걸 쏟았다\n크루 전원 피 땀 눈물\n4월 26일 기다려라', likes: '3.4K', comments: 688, time: '04. 19. 22:00' },
  { id: 602, artistId: 6, author: 'COSM', body: 'CYPHER 녹음하면서\n오랜만에 진짜 재밌었다\n다음 콜라보도 곧 나올 거니까\n기대해', likes: '2.8K', comments: 512, time: '04. 13. 20:30' },
  { id: 603, artistId: 6, author: 'LOW-G', body: 'NO CEILING 투어\n작은 무대라도 전국 다 간다\n한 놈도 빠짐없이 만나러 간다', likes: '2.2K', comments: 412, time: '04. 01. 19:00' },
);

Object.assign(window, {
  NOTICES, FAN_LETTERS, HIGHLIGHTS, LIVE_REPLAYS, MEDIA_EXTENDED,
  JELLY_PACKS, HERO_SLIDES, ON_LIVE_NOW, FAN_POSTS, ARTIST_POSTS,
});

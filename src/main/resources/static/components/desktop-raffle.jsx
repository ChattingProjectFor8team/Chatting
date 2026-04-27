// Connectfin — Desktop Raffle entry wrapper
// 데스크톱에 래플 진입점이 없어 임시로 모바일 컴포넌트를 중앙 정렬 컬럼에 재사용한다.
// 데스크톱 네이티브 그리드 디자인은 후속 이슈에서 별도 구현.

function DesktopRaffleScreen({ t, theme, onArtistOpen }) {
  const [view, setView] = React.useState('list'); // 'list' | 'detail'
  const [raffleId, setRaffleId] = React.useState(null);

  return (
    <div style={{
      position: 'relative',
      width: '100%',
      maxWidth: 520,
      margin: '0 auto',
      height: 'calc(100vh - 56px)',
      overflow: 'hidden',
      background: t.bg,
      borderLeft: `1px solid ${t.line}`,
      borderRight: `1px solid ${t.line}`,
    }}>
      {view === 'list' && (
        <RaffleListScreen
          t={t}
          onBack={() => {}}
          onOpenRaffle={(id) => { setRaffleId(id); setView('detail'); }}
        />
      )}
      {view === 'detail' && raffleId != null && (
        <RaffleDetailScreen
          t={t}
          theme={theme}
          raffleId={raffleId}
          onBack={() => setView('list')}
          onEnter={() => setView('list')}
        />
      )}
    </div>
  );
}

Object.assign(window, { DesktopRaffleScreen });

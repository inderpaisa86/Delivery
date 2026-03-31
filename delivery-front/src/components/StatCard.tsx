interface Props { label: string; value: number | string; icon: string; color?: string; }

export function StatCard({ label, value, icon, color = 'from-orange-500 to-orange-600' }: Props) {
  return (
    <div className="bg-white rounded-2xl p-5 shadow-[var(--shadow-md)] border border-[var(--border)] hover:shadow-[var(--shadow-lg)] transition-shadow">
      <div className="flex items-center gap-4">
        <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${color} flex items-center justify-center text-xl text-white shadow-sm`}>{icon}</div>
        <div><p className="text-3xl font-bold">{value}</p><p className="text-sm text-[var(--text-muted)]">{label}</p></div>
      </div>
    </div>
  );
}

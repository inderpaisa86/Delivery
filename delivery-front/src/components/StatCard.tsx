interface Props {
  label: string;
  value: number | string;
  icon: string;
  color?: string;
}

export function StatCard({ label, value, icon, color = 'bg-indigo-50 text-indigo-700' }: Props) {
  return (
    <div className={`rounded-xl p-4 ${color} flex items-center gap-3`}>
      <span className="text-2xl">{icon}</span>
      <div>
        <p className="text-2xl font-bold">{value}</p>
        <p className="text-sm opacity-75">{label}</p>
      </div>
    </div>
  );
}

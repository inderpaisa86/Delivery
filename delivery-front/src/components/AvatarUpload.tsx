import { useRef, useState } from 'react';

export const DEFAULT_AVATAR = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" fill="none">
  <rect width="100" height="100" rx="50" fill="#E0E7FF"/>
  <circle cx="50" cy="38" r="16" fill="#818CF8"/>
  <ellipse cx="50" cy="78" rx="28" ry="20" fill="#818CF8"/>
</svg>`);

interface Props {
  value: string | null;
  onChange: (base64: string | null) => void;
  size?: number;
}

export function AvatarUpload({ value, onChange, size = 96 }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(value);

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) return;
    if (file.size > 2 * 1024 * 1024) return;

    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result as string;
      setPreview(base64);
      onChange(base64);
    };
    reader.readAsDataURL(file);
  };

  const remove = () => {
    setPreview(null);
    onChange(null);
    if (inputRef.current) inputRef.current.value = '';
  };

  return (
    <div className="flex flex-col items-center gap-2">
      <div
        className="relative group cursor-pointer"
        onClick={() => inputRef.current?.click()}
        style={{ width: size, height: size }}
      >
        <img
          src={preview || DEFAULT_AVATAR}
          alt="Foto"
          className="w-full h-full rounded-full object-cover border-2 border-gray-200 group-hover:border-indigo-400 transition-all"
        />
        <div className="absolute inset-0 rounded-full bg-black/30 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center">
          <span className="text-white text-xs font-medium">📷 Cambiar</span>
        </div>
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        onChange={handleFile}
        className="hidden"
      />
      {preview && (
        <button type="button" onClick={remove} className="text-xs text-red-500 hover:underline">
          Quitar foto
        </button>
      )}
      <p className="text-xs text-gray-400">Máx. 2MB</p>
    </div>
  );
}

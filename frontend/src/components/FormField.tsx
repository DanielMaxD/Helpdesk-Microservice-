interface FormFieldProps {
  label: string;
  id: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
}

export default function FormField({
  label,
  id,
  type = "text",
  value,
  onChange,
  placeholder,
  autoComplete,
  required = true,
}: FormFieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="font-body text-sm font-medium text-ink-950">
        {label}
      </label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required={required}
        className="rounded-lg border border-slate-200 bg-paper-0 px-3.5 py-2.5 font-body text-sm text-ink-950 placeholder:text-slate-400 transition-colors focus:border-indigo focus:outline-none focus:ring-2 focus:ring-indigo-100"
      />
    </div>
  );
}

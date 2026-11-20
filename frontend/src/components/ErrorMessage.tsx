import { ApiError } from '../api/client';

/** Shows an error, including the field errors of a backend validation failure. */
export function ErrorMessage({ error }: { error: Error | undefined }) {
  if (!error) {
    return null;
  }
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : [];
  return (
    <div className="error" role="alert">
      <p>{error.message}</p>
      {fieldErrors.length > 0 && (
        <ul>
          {fieldErrors.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

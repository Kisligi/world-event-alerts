import { useState } from 'react';
import { api } from '../../api/client';
import type { ChannelStatus } from '../../api/types';
import { ErrorMessage } from '../../components/ErrorMessage';
import { channelLabel } from '../../format';
import { useApi } from '../../useApi';

/** Global on/off per channel (ADR-026): while off, its deliveries are logged as Skipped. */
export function ChannelsPage() {
  const { data: channels, error: loadError, reload } = useApi(api.listChannels, 'channels');
  const [busy, setBusy] = useState<ChannelStatus['type']>();
  const [error, setError] = useState<Error>();

  async function toggle(channel: ChannelStatus) {
    setBusy(channel.type);
    setError(undefined);
    try {
      await api.setChannelEnabled(channel.type, !channel.enabled);
      reload();
    } catch (e) {
      setError(e as Error);
    } finally {
      setBusy(undefined);
    }
  }

  return (
    <>
      <p className="lead">
        Switch a channel off for everyone, for example during a provider outage. Notifications for a switched-off
        channel are not sent; they appear in the delivery log as Skipped.
      </p>
      <ErrorMessage error={loadError ?? error} />
      <div className="grid">
        {channels?.map((channel) => (
          <article key={channel.type} className="card channel-card" aria-label={channelLabel(channel.type)}>
            <header>
              <h2>{channelLabel(channel.type)}</h2>
              <span className={`badge ${channel.enabled ? 'on' : 'off'}`}>{channel.enabled ? 'On' : 'Off'}</span>
            </header>
            {!channel.implemented && <p className="error">No implementation is deployed for this channel.</p>}
            <button
              type="button"
              className={channel.enabled ? 'secondary' : undefined}
              onClick={() => toggle(channel)}
              disabled={busy === channel.type}
            >
              {channel.enabled ? `Switch ${channelLabel(channel.type)} off` : `Switch ${channelLabel(channel.type)} on`}
            </button>
          </article>
        ))}
      </div>
    </>
  );
}

package com.tangxiaolv.telegramgallery.secretmedia;

import android.content.Context;

import com.tangxiaolv.telegramgallery.exoplayer2.upstream.DataSource;
import com.tangxiaolv.telegramgallery.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.tangxiaolv.telegramgallery.exoplayer2.upstream.TransferListener;

public final class ExtendedDefaultDataSourceFactory implements DataSource.Factory {

    private final Context context;
    private final TransferListener<? super DataSource> listener;
    private final DataSource.Factory baseDataSourceFactory;

    /**
     * @param context A context.
     * @param userAgent The User-Agent string that should be used.
     */
    public ExtendedDefaultDataSourceFactory(Context context, String userAgent) {
        this(context, userAgent, null);
    }

    /**
     * @param context A context.
     * @param userAgent The User-Agent string that should be used.
     * @param listener An optional listener.
     */
    public ExtendedDefaultDataSourceFactory(Context context, String userAgent,
                                    TransferListener<? super DataSource> listener) {
        this(context, listener, new DefaultHttpDataSourceFactory(userAgent, listener));
    }

    /**
     * @param context A context.
     * @param listener An optional listener.
     * @param baseDataSourceFactory A {@link DataSource.Factory} to be used to create a base {@link DataSource}
     *     for {@link DefaultDataSource}.
     * @see DefaultDataSource#DefaultDataSource(Context, TransferListener, DataSource)
     */
    public ExtendedDefaultDataSourceFactory(Context context, TransferListener<? super DataSource> listener,
                                    DataSource.Factory baseDataSourceFactory) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.baseDataSourceFactory = baseDataSourceFactory;
    }

    @Override
    public ExtendedDefaultDataSource createDataSource() {
        return new ExtendedDefaultDataSource(context, listener, baseDataSourceFactory.createDataSource());
    }
}
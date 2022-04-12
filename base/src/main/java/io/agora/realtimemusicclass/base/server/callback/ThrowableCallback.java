package io.agora.realtimemusicclass.base.server.callback;

import androidx.annotation.Nullable;

public interface ThrowableCallback<T> extends NetworkCallback<T> {
    void onFailure(@Nullable Throwable throwable);
}

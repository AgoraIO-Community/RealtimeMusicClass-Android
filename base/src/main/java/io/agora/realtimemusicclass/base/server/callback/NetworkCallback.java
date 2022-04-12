package io.agora.realtimemusicclass.base.server.callback;

import androidx.annotation.Nullable;

public interface NetworkCallback<T> {
    void onSuccess(@Nullable T res);
}

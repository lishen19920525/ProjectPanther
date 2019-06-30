package io.panther;

import android.support.annotation.Nullable;

import io.reactivex.observers.DisposableObserver;

/**
 * Desc:
 * Project: ProjectPanther
 * Author: LiShen
 * Time: 2019-06-30 16:55
 */
public abstract class ValueObserver<T> extends DisposableObserver<T> {
    public abstract void onResult(@Nullable T result);

    @Override
    public final void onNext(T t) {
        onResult(t);
    }

    @Override
    public final void onError(Throwable e) {
        onResult(null);
    }

    @Override
    public final void onComplete() {

    }
}
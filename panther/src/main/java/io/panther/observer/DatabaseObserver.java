package io.panther.observer;

import android.support.annotation.Nullable;

import io.reactivex.observers.DisposableObserver;

/**
 * Data callback
 * Project: ProjectPanther
 * Author: LiShen
 * Time: 2019/4/3 13:20
 */
public abstract class DatabaseObserver<T> extends DisposableObserver<T> {
    public abstract void onResult(@Nullable T value);

    @Override
    public final void onNext(T o) {
        onResult(o);
    }

    @Override
    public final void onError(Throwable e) {
        onResult(null);
    }

    @Override
    public final void onComplete() {

    }
}

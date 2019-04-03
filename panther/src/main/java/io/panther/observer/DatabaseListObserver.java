package io.panther.observer;

import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.observers.DisposableObserver;

/**
 * Data list callback
 * Project: ProjectPanther
 * Author: LiShen
 * Time: 2019/4/3 13:20
 */
public abstract class DatabaseListObserver<T> extends DisposableObserver {
    public abstract void onResult(@Nullable List<T> value);

    @Override
    public final void onNext(Object o) {
        onResult((List<T>) o);
    }

    @Override
    public final void onError(Throwable e) {
        onResult(null);
    }

    @Override
    public final void onComplete() {

    }
}
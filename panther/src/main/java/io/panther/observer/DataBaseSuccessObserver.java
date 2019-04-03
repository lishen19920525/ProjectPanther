package io.panther.observer;

import io.reactivex.observers.DisposableObserver;

/**
 * Operation callback
 * Project: ProjectPanther
 * Author: LiShen
 * Time: 2019/4/3 13:24
 */
public abstract class DataBaseSuccessObserver extends DisposableObserver<Boolean> {
    public abstract void onResult(boolean success);

    @Override
    public void onNext(Boolean aBoolean) {
        onResult(aBoolean);
    }

    @Override
    public void onError(Throwable e) {
        onResult(false);
    }

    @Override
    public void onComplete() {

    }
}

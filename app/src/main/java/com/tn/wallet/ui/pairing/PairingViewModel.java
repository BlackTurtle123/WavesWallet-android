package com.tn.wallet.ui.pairing;

import android.support.annotation.StringRes;

import com.tn.wallet.R;
import com.tn.wallet.injection.Injector;
import com.tn.wallet.ui.base.BaseViewModel;
import com.tn.wallet.ui.customviews.ToastCustom;
import com.tn.wallet.util.AddressUtil;
import com.tn.wallet.util.AppUtil;
import com.tn.wallet.util.PrefsUtil;

import javax.inject.Inject;

public class PairingViewModel extends BaseViewModel {

    @Inject protected AppUtil appUtil;
    @Inject protected PrefsUtil prefsUtil;

    private DataListener dataListener;

    interface DataListener {

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog(@StringRes int message);

        void dismissProgressDialog();

        void startSeedWalletActivity(String seed);

    }

    PairingViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
    }

    private boolean isLooksLikeAddress(String seed) {
        return AddressUtil.isWavesUri(seed) || AddressUtil.isValidAddress(seed);
    }

    void pairWithQR(String raw) {
        if (dataListener != null) {
            if (isLooksLikeAddress(raw)) {
                dataListener.showToast(R.string.seed_like_address, ToastCustom.TYPE_ERROR);
            } else {
                dataListener.startSeedWalletActivity(raw);
            }
        }
    }

}

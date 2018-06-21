package com.tn.wallet.payload;

import com.tn.wallet.api.NodeManager;
import com.tn.wallet.util.MoneyUtil;

public class ReissueTransaction extends Transaction {
    public String assetId;
    public long quantity;
    public boolean reissuable;

    public ReissueTransaction(int type, String id, String sender, String assetId, long timestamp,
                              long amount, long fee, long quantity, boolean reissuable) {
        super(type, id, sender, timestamp, amount, fee);
        this.assetId = assetId;
        this.quantity = quantity;
        this.reissuable = reissuable;
    }

    @Override
    public String getAssetName() {
        return assetId != null ? NodeManager.get().getAssetName(assetId) : super.getAssetName();
    }

    @Override
    public boolean isForAsset(String assetId) {
        return this.assetId.equals(assetId);
    }

    @Override
    public int getDecimals() {
        return assetId != null ? NodeManager.get().getAssetBalance(assetId).issueTransaction.decimals : getDecimals();
    }

    @Override
    public String getDisplayAmount() {
        return MoneyUtil.getScaledText(quantity, getDecimals());
    }

    @Override
    public int getDirection() {
        return RECEIVED;
    }
}

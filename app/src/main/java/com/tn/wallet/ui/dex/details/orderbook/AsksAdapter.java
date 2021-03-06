package com.tn.wallet.ui.dex.details.orderbook;

import android.support.v4.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.tn.wallet.R;
import com.tn.wallet.payload.OrderBook;
import com.tn.wallet.payload.WatchMarket;
import com.tn.wallet.util.MoneyUtil;

public class AsksAdapter extends BaseQuickAdapter<OrderBook.Asks, BaseViewHolder> {
    private WatchMarket mTickerMarket;

    public AsksAdapter() {
        super(R.layout.order_book_item, null);
    }

    @Override
    protected void convert(BaseViewHolder baseViewHolder, OrderBook.Asks asks) {
        baseViewHolder.setText(R.id.text_value, MoneyUtil.getTextStripZeros(MoneyUtil.getScaledText(Long.valueOf(asks.amount), mTickerMarket.market.getAmountAssetInfo().decimals)))
                .setBackgroundColor(R.id.relative_block, getData().indexOf(asks) % 2 == 0 ? ContextCompat.getColor(mContext,R.color.dex_orderbook_right_bg) : ContextCompat.getColor(mContext,R.color.dex_orderbook_right_bg_lighter));
    }

    public void setTickerMarket(WatchMarket tickerMarket) {
        mTickerMarket = tickerMarket;
    }
}
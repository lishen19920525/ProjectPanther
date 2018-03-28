package io.panther.demo.bean;

import java.math.BigDecimal;

import io.panther.demo.CurrencyUtils;
import io.panther.demo.MD5Utils;

/**
 * Created by LiShen on 2018/3/21.
 * 交易单簿
 */

public class TradeBookBean extends BaseBean {
    private String id;
    private long createTime;
    private String tradeUnitCurrency;//交易单位USDT
    private String tradeCurrency;///交易目标ETH
    private BigDecimal currentProfit;//目前利润单位USDT
    private BigDecimal positions;//持仓量 ETH >=0
    private long updateTime;

    public TradeBookBean() {
        createTime = System.currentTimeMillis();
        currentProfit = CurrencyUtils.zero();
        positions = CurrencyUtils.zero();

        id = "trade_book_" + MD5Utils.to16Lower(String.valueOf(createTime));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getTradeUnitCurrency() {
        return tradeUnitCurrency;
    }

    public void setTradeUnitCurrency(String tradeUnitCurrency) {
        this.tradeUnitCurrency = tradeUnitCurrency;
    }

    public String getTradeCurrency() {
        return tradeCurrency;
    }

    public void setTradeCurrency(String tradeCurrency) {
        this.tradeCurrency = tradeCurrency;
    }

    public BigDecimal getCurrentProfit() {
        return currentProfit;
    }

    public void setCurrentProfit(BigDecimal currentProfit) {
        this.currentProfit = currentProfit;
    }

    public BigDecimal getPositions() {
        return positions;
    }

    public void setPositions(BigDecimal positions) {
        this.positions = positions;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
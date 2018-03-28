package io.panther.demo.bean;

import java.math.BigDecimal;

import io.panther.demo.CONSTANT;
import io.panther.demo.CurrencyUtils;
import io.panther.demo.MD5Utils;

/**
 * Created by LiShen on 2018/3/21.
 * 交易单
 */

public class TradeBean extends BaseBean {
    private String id;
    private int type;//1买入 2卖出
    private long time;//交易时间
    private String tradeUnitCurrency;//交易单位USDT
    private String tradeCurrency;//交易目标ETH
    private BigDecimal price; // 交易单价**USDT
    private BigDecimal quantity;//交易数量
    private BigDecimal amount;// 交易总量 **USDT
    private BigDecimal positions;// 单量持有**ETH type 为1 才有 >=0

    public TradeBean() {
        type = CONSTANT.TradeType.BUYING;
        time = System.currentTimeMillis();
        price = CurrencyUtils.zero();
        quantity = CurrencyUtils.zero();
        amount = CurrencyUtils.zero();
        positions = CurrencyUtils.zero();

        id = "trade_" + MD5Utils.to16Lower(String.valueOf(time));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPositions() {
        return positions;
    }

    public void setPositions(BigDecimal positions) {
        this.positions = positions;
    }
}
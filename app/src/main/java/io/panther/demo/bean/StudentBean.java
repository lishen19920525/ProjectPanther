package io.panther.demo.bean;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by LiShen on 2018/3/19.
 * ProjectPanther
 */

public class StudentBean {
    private Long id;
    private String name;
    private Date birthday;
    private BigDecimal cardBalance;
    private Gender gender;
    private Boolean valid;
    private Integer grade;
    private Float credit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public BigDecimal getCardBalance() {
        return cardBalance;
    }

    public void setCardBalance(BigDecimal cardBalance) {
        this.cardBalance = cardBalance;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public Float getCredit() {
        return credit;
    }

    public void setCredit(Float credit) {
        this.credit = credit;
    }

    @Override
    public String toString() {
        return "StudentBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", birthday=" + birthday.toLocaleString() +
                ", cardBalance=" + cardBalance.toPlainString() +
                ", gender=" + gender.toString() +
                ", valid=" + valid +
                ", grade=" + grade +
                ", credit=" + credit +
                '}';
    }
}
package io.panther.demo;

import java.util.Date;

/**
 * Created by LiShen on 2018/3/19.
 * ProjectPanther
 */

public class StudentBean {
    private Long id;
    private Integer gender;
    private String name;
    private Date birthday;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
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

    @Override
    public String toString() {
        return "StudentBean{" +
                "id=" + id +
                ", gender=" + gender +
                ", name='" + name + '\'' +
                ", birthday=" + birthday +
                '}';
    }
}
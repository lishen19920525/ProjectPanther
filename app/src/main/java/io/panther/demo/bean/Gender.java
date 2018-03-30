package io.panther.demo.bean;

/**
 * Created by LiShen on 2018/3/29.
 * ProjectPanther
 */

public enum Gender {
    FEMALE {
        @Override
        public String toString() {
            return "female";
        }
    },
    MALE{
        @Override
        public String toString() {
            return "male";
        }
    }
}

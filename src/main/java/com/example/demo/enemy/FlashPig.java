package com.example.demo.enemy;

import java.util.List;

public class FlashPig extends Enemy{
    public FlashPig(){
        super("闪电猪",70,true,false, true,List.of(
                new Step(Intent.ATTACK,12),
                new Step(Intent.DEFEND,5),
                new Step(Intent.BUFF,3),
                new Step(Intent.ATTACK ,15)
        ));
    }
    public boolean Dodge(){
        double evasion=Math.random();
        if(evasion>0.6 ){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public boolean hasDodge() {
        return true;
    }

    @Override
    public boolean dodge() {
        return Dodge();
    }
}

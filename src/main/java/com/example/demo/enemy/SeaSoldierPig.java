package com.example.demo.enemy;

import java.util.List;

public class SeaSoldierPig extends Enemy{
    public SeaSoldierPig(){
        super("海兵猪",45,true,false, List.of(
           new Step(Intent.DEFEND,20),
           new Step(Intent.WET,1),
           new Step(Intent.ATTACK,12),
           new Step(Intent.BUFF,3)
        ));
        portraitName = "海兵猪";
        portraitSize = 180;              // 贴图更小
    }
}

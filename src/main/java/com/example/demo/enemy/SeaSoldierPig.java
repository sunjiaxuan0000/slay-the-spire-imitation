package com.example.demo.enemy;

import java.util.List;

public class SeaSoldierPig extends Enemy{
    public SeaSoldierPig(){
        super("海兵猪",80,true,false, List.of(
           new Step(Intent.DEFEND,20),
           new Step(Intent.WET,1),
           new Step(Intent.ATTACK,20),
           new Step(Intent.BUFF,3)
        ));
        portraitName = "猪龙鱼公爵";  // 复用猪龙鱼公爵一阶段立绘
        portraitSize = 180;              // 贴图更小
    }
}

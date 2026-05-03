package dev.dubhe.bonded.data;

import dev.dubhe.bonded.BondedPeaks;
import dev.dubhe.bonded.data.lang.BondedPeaksLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = BondedPeaks.MOD_ID)
public class BondedPeaksData {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(true, new BondedPeaksLangProvider(packOutput));
    }
}

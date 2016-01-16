/*
 * Copyright (c) 2014. Dries K. Aka Dries007
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.dries007.holoInventory.client;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.dries007.holoInventory.HoloInventory;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyManager
{
    public static final KeyBinding key = new KeyBinding("HoloInventory", 0, "key.categories.misc")
    {
        @Override
        public void setKeyCode(int p_151462_1_)
        {
            HoloInventory.getConfig().setKey(p_151462_1_);
            super.setKeyCode(p_151462_1_);
        }
    };

    public KeyManager()
    {
        ClientRegistry.registerKeyBinding(key);
        key.setKeyCode(HoloInventory.getConfig().getKey());
        switch (HoloInventory.getConfig().keyMode)
        {
            case 1:
                Renderer.INSTANCE.enabled = HoloInventory.getConfig().keyState;
                break;
            case 2:
                Renderer.INSTANCE.enabled = false;
                break;
        }
    }

    /**
     * Valid modes:
     * 0: Always display hologram.
     * 1: The key toggles the rendering.
     * 2: Only render hologram while key pressed. (Handled in Renderer)
     * 3: Don't render hologram while key pressed. (Handled in Renderer)
     */

    boolean alreadyToggling = false;

    @SubscribeEvent
    public void input(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) return;
        int code = key.getKeyCode();
        if (code < 0 || code > Keyboard.KEYBOARD_SIZE) return;
        switch (HoloInventory.getConfig().keyMode)
        {
            case 1:
                if (Keyboard.isKeyDown(code))
                {
                    if (!alreadyToggling)
                    {
                        alreadyToggling = true;
                        Renderer.INSTANCE.enabled = !Renderer.INSTANCE.enabled;
                        HoloInventory.getConfig().setKeyState(Renderer.INSTANCE.enabled);
                    }
                }
                else alreadyToggling = false;
                break;
            case 2:
                Renderer.INSTANCE.enabled = Keyboard.isKeyDown(code);
                break;
            case 3:
                Renderer.INSTANCE.enabled = !Keyboard.isKeyDown(code);
                break;
        }
    }
}

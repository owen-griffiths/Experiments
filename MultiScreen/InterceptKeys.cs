using System;
using System.Diagnostics;
using System.Drawing;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows.Forms;

namespace MultiScreen
{
    static class InterceptKeys
    {
        public enum Reason {UP, DOWN};

        public delegate void OnKeyEvent(Reason reason, Keys key, bool altIsDown, bool ctrlIsDown);

        public static void Init(OnKeyEvent handler) 
        {
            _handler = handler;
            _hookID = SetHook(_proc);
        }

        public static void Shutdown()
        {
            UnhookWindowsHookEx(_hookID);
        }

        private static LowLevelKeyboardProc _proc = HookCallback;
        private static IntPtr _hookID = IntPtr.Zero;
        private static OnKeyEvent _handler;

        private const int WH_KEYBOARD_LL = 13;
        private const int WM_KEYDOWN = 0x0100;
        private const int WM_KEYUP = 0x0101;

        private static IntPtr SetHook(LowLevelKeyboardProc proc)
        {
            using (Process curProcess = Process.GetCurrentProcess())
            using (ProcessModule curModule = curProcess.MainModule)
            {
                return SetWindowsHookEx(WH_KEYBOARD_LL, proc,
                    GetModuleHandle(curModule.ModuleName), 0);
            }
        }

        private delegate IntPtr LowLevelKeyboardProc(
            int nCode, IntPtr wParam, IntPtr lParam);

        private static IntPtr HookCallback(
            int nCode, IntPtr wParam, IntPtr lParam)
        {
            if (nCode >= 0)
            {
                Reason? reason = null;
                if (wParam == (IntPtr)WM_KEYDOWN)
                {
                    reason = Reason.DOWN;
                }
                else if (wParam == (IntPtr)WM_KEYUP)
                {
                    reason = Reason.UP;
                }
                if (reason.HasValue)
                {
                    int vkCode = Marshal.ReadInt32(lParam);
                    Keys key = (Keys)vkCode;
                    bool altIsDown = KeyIsDown(Keys.Menu);
                    bool ctrlIsDown = KeyIsDown(Keys.ControlKey);
                    _handler(reason.Value, key, altIsDown, ctrlIsDown);
                }
            }
            return CallNextHookEx(_hookID, nCode, wParam, lParam);
        }

        private static bool KeyIsDown(Keys key)
        {
            ushort result = GetAsyncKeyState((int)key);
            return (result & 0x8000) != 0;
        }

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern IntPtr SetWindowsHookEx(int idHook,
            LowLevelKeyboardProc lpfn, IntPtr hMod, uint dwThreadId);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool UnhookWindowsHookEx(IntPtr hhk);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode,
            IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern ushort GetAsyncKeyState(int vKey);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern IntPtr GetModuleHandle(string lpModuleName);
    }
}

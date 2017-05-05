using System;
using System.Drawing;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace MultiScreen
{
    class MultiScreenMain
    {
        static void Main(string[] args)
        {
            foreach (var s in Screen.AllScreens)
            {
                Console.WriteLine("{0}: {1}", s.DeviceName, s.WorkingArea);
            }

            var keyboardListener = new Thread(HandleKeys);
            keyboardListener.Start();

            Console.WriteLine("Press 'Q' to exit");
            for (;;)
            {
                var pressed = Console.ReadKey();
                if (char.ToUpper(pressed.KeyChar) == 'Q')
                {
                    break;
                }
            }

            Console.WriteLine("Removing hook");
            InterceptKeys.Shutdown();

            Application.Exit();
            keyboardListener.Join();
        }

        private static void HandleKeys()
        {
            Console.WriteLine("Installing hook");
            InterceptKeys.Init(OnKeyEvent);

            Console.WriteLine("Starting message loop");
            Application.Run();

            Console.WriteLine("Message loop ended");
        }

        private static void OnKeyEvent(InterceptKeys.Reason reason, Keys key, bool altIsDown, bool ctrlIsDown)
        {
            //Console.WriteLine("{0} {1} Alt:{2} Ctrl:{3}", key, reason, altIsDown, ctrlIsDown);

            if (reason == InterceptKeys.Reason.DOWN)
            {
                if (altIsDown && ctrlIsDown)
                {
                    if ((key >= Keys.D1) && (key <= Keys.D9))
                    {
                        state.OnNumericKey(key);
                    }
                    else if ((key >= Keys.NumPad1) && (key <= Keys.NumPad9))
                    {
                        state.OnNumericKey(key);
                    }
                    else if (key == Keys.Add)
                    {
                        state.OnPlus();
                    }
                    else if (key == Keys.Subtract)
                    {
                        Application.Exit();
                    }
                }
            }
            // If lifting either of 
            else if ((key == Keys.LControlKey) || (key == Keys.LMenu))
            {
                Rectangle? toCover = state.AreaToCover;
                if (toCover.HasValue)
                {
                    Console.WriteLine($"Targeting Screen {state.TargetScreen}");
                    DumpArea(toCover.Value);
                    SetActiveWindowPosition(toCover.Value, state.TargetScreen);
                    Console.WriteLine();
                }

                state = new State();
            }
        }

        private static void DumpArea(Rectangle toCover)
        {
            for (int y = 0; y < 6; y++)
            {
                for (int x = 0; x < 6; x++)
                    Console.Write(toCover.Contains(x, y) ? '*' : '.');
                Console.WriteLine();
            }
        }

        public static void SetActiveWindowPosition(Rectangle toCover, int targetScreen)
        {
            int iScreen = Math.Min(targetScreen, Screen.AllScreens.Length - 1);
            Screen s = Screen.AllScreens[iScreen];
            Rectangle whole = s.WorkingArea;

            int left = whole.Left + (toCover.Left * whole.Width) / 6;
            int right = whole.Left + (toCover.Right * whole.Width) / 6;

            int top = whole.Top + (toCover.Top * whole.Height) / 6;
            int bottom = whole.Top + (toCover.Bottom * whole.Height) / 6;

            var foregroundWindow = GetForegroundWindow();
            StringBuilder targetWindowTitle = new StringBuilder(132);
            GetWindowText(foregroundWindow, targetWindowTitle, targetWindowTitle.Capacity);

            Console.WriteLine($"Moving window titled '{targetWindowTitle}'");

            SetWindowPos(GetForegroundWindow(), IntPtr.Zero, left, top, right - left, bottom - top, 0);
        }


        private static State state;

        [DllImport("user32.dll")]
        public static extern int SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int x, int y, int width, int height, uint flags);

        [DllImport("user32.dll")]
        static extern IntPtr GetForegroundWindow();

        [DllImport("user32.dll")]
        static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);
    }
}


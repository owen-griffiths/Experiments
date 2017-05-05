using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;

namespace MultiScreen
{
    public struct State
    {
        private int countNumKeys;
        private Rectangle? areaSingle;
        private Rectangle? areaMulti;
        private int countPlus;

        public void OnNumericKey(Keys key)
        {
            countNumKeys++;

            areaMulti = Union(areaMulti, numericKeyRegionsMulti[key]);
            areaSingle = Union(areaSingle, numericKeyRegionsSingle[key]);
        }

        public void OnPlus()
        {
            countPlus++;
        }

        public int TargetScreen => countPlus;

        public Rectangle? AreaToCover
        {
            get
            {
                if (countNumKeys == 1)
                {
                    return areaSingle.Value;
                }
                else if (countNumKeys > 1)
                {
                    return areaMulti.Value;
                }
                return null;
            }
        }

        private static Dictionary<Keys, Rectangle> numericKeyRegionsMulti = new Dictionary<Keys, Rectangle>()
        {
            {Keys.NumPad1, new Rectangle(0, 4, 2, 2)},
            {Keys.NumPad2, new Rectangle(2, 4, 2, 2)},
            {Keys.NumPad3, new Rectangle(4, 4, 2, 2)},

            {Keys.NumPad4, new Rectangle(0, 2, 2, 2)},
            {Keys.NumPad5, new Rectangle(2, 2, 2, 2)},
            {Keys.NumPad6, new Rectangle(4, 2, 2, 2)},

            {Keys.NumPad7, new Rectangle(0, 0, 2, 2)},
            {Keys.NumPad8, new Rectangle(2, 0, 2, 2)},
            {Keys.NumPad9, new Rectangle(4, 0, 2, 2)},

            {Keys.D1, new Rectangle(0, 4, 2, 2)},
            {Keys.D2, new Rectangle(2, 4, 2, 2)},
            {Keys.D3, new Rectangle(4, 4, 2, 2)},

            {Keys.D4, new Rectangle(0, 2, 2, 2)},
            {Keys.D5, new Rectangle(2, 2, 2, 2)},
            {Keys.D6, new Rectangle(4, 2, 2, 2)},

            {Keys.D7, new Rectangle(0, 0, 2, 2)},
            {Keys.D8, new Rectangle(2, 0, 2, 2)},
            {Keys.D9, new Rectangle(4, 0, 2, 2)},
        };

        private static Dictionary<Keys, Rectangle> numericKeyRegionsSingle = new Dictionary<Keys, Rectangle>()
        {
            // Quarters
            {Keys.NumPad7, new Rectangle(0, 0, 3, 3)},
            {Keys.NumPad9, new Rectangle(3, 0, 3, 3)},
            {Keys.NumPad1, new Rectangle(0, 3, 3, 3)},
            {Keys.NumPad3, new Rectangle(3, 3, 3, 3)},

            {Keys.D7, new Rectangle(0, 0, 3, 3)},
            {Keys.D9, new Rectangle(3, 0, 3, 3)},
            {Keys.D1, new Rectangle(0, 3, 3, 3)},
            {Keys.D3, new Rectangle(3, 3, 3, 3)},

            // Horizonal half
            {Keys.NumPad8, new Rectangle(0, 0, 6, 3)},
            {Keys.NumPad2, new Rectangle(0, 3, 6, 3)},

            {Keys.D8, new Rectangle(0, 0, 6, 3)},
            {Keys.D2, new Rectangle(0, 3, 6, 3)},

            // Vertical half
            {Keys.NumPad4, new Rectangle(0, 0, 3, 6)},
            {Keys.NumPad6, new Rectangle(3, 0, 3, 6)},

            {Keys.D4, new Rectangle(0, 0, 3, 6)},
            {Keys.D6, new Rectangle(3, 0, 3, 6)},

            // Whole
            {Keys.NumPad5, new Rectangle(0, 0, 6, 6)},
            {Keys.D5, new Rectangle(0, 0, 6, 6)},
        };

        private static Rectangle Union(Rectangle? a, Rectangle b)
        {
            return a.HasValue ? Rectangle.Union(a.Value, b) : b;
        }
    }
}

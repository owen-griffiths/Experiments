   M S B A M L   `   `   `  ����  WpfTextView    WpfTextView.FileView   5      \ XPresentationFramework, Version=4.0.0.0, Culture=neutral, PublicKeyToken=31bf3856ad364e35R NWindowsBase, Version=4.0.0.0, Culture=neutral, PublicKeyToken=31bf3856ad364e35W SPresentationCore, Version=4.0.0.0, Culture=neutral, PublicKeyToken=31bf3856ad364e35D 9http://schemas.microsoft.com/winfx/2006/xaml/presentation    5      R NSystem.Xaml, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089:x,http://schemas.microsoft.com/winfx/2006/xaml     5      Jmc;http://schemas.openxmlformats.org/markup-compatibility/2006     5      @d2http://schemas.microsoft.com/expression/blend/2008     5      -   5        .��5      �   � RowDefinitions  5	   
   �� 5
      $��* �6   �� 5      $
��Auto �6   �� 5      -   5         �Name$ FindRow��5      $��0 �6+   .+�5   
   �� -   5        $��textView��5      $��0��5      $ �� IsSynchronizedWithCurrentItem$
 TrueY�5      $��Stretch=�5      $��Stretch=�6+   $
��Auto��6G   $��0,0,0,0q�6T    �� SelectionMode$ Single=�5      $��Courier New)�6$   ��5      ��  �� Mode$ OneWay=� �� Path$ Contents�5   $   �� 5   
   -   5        $��splitter��5       �� ResizeDirection$
 Rows=�5      $��1��6.   $
��Auto��5      $��Stretch=�6$   $��3��5      $��Stretch=�6"   $��0q�5      �� 5   
   -   5        $��findResults��5      $��2��5      $��Stretch=�5      $
��Auto��60   $��Stretch=�5      $
��Auto��6.   $��0,0,0,0q�5       �� 5"   
   -   5        $��
findButton��5"      $
��Find��6#   $��2��5#      $��23��5$      $
��Left=�6   $	��Top=�68   $��44��6P    �� 	IsDefault . {�5%      �� $ CanStartFind��� 5'   
   -   5        $��
stopButton��5'       $
��Stop��6   $��23��5(      $
��Left=�6    .  $��50,0,0,0q�6J   $	��Top=�6\   $��44��6t   $��2��6   {�5)      �� $ FindIsInProgress�6=   �� 5+   
   -   5        $��findText��5+      $��2��5,      $��23��5-      $	��Top=�6   $��Stretch=�5.      $��100,0,109,0q�60   �� 50   
   -	   5        $��	listView1��50      $��2��51      $��
0,29,0,-29q�52      $��Stretch=�6!   $��Stretch=�53      $
��Auto��6+   $ Single=�54   d   ��6   �� $ FindResults� �� View 56      �� 57      .P�58      �� $
��Line��6%   	 �� Width$	 80��6_   
 �� DisplayMemberBinding
 63   �� *
LineNumber+6_   �� 59      $��Contents��6%   $
	 1600��6]   
 67   �� *Line+6]   5:      5<      � 5=   
   -
   5        $��findPercent��5=   ^   $��2��6   $��23��6#   $��Right=�6/   $��	700,0,0,0q�6K    � Maximum$	 100Q�6q   $	��Top=�6   $	��100��6�    � Value 6�   �� $ FindPercent�5>      5?      
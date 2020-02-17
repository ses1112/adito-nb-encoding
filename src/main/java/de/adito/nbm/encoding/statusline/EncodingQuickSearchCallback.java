package de.adito.nbm.encoding.statusline;

import de.adito.swing.quicksearch.IExtendedQuickSearchCallback;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * @author m.kaspera, 14.02.2020
 */
public class EncodingQuickSearchCallback implements IExtendedQuickSearchCallback
{

  private final JList<String> encodingList;
  private String searchString = null;
  private int searchIndex = 0;

  public EncodingQuickSearchCallback(JList<String> pEncodingList)
  {
    encodingList = pEncodingList;
  }

  @Override
  public void quickSearchUpdate(String pSearchText)
  {
    searchString = pSearchText;
    searchIndex = 0;
    if (searchString != null)
    {
      _goToFoundLine(_findNthOccurrence(searchString, searchIndex));
    }
  }

  @Override
  public void showNextSelection(boolean forward)
  {
    if (forward)
      searchIndex++;
    else
      searchIndex--;
    _goToFoundLine(_findNthOccurrence(searchString, searchIndex));
  }

  @Override
  public String findMaxPrefix(String prefix)
  {
    return null;
  }

  @Override
  public void quickSearchConfirmed()
  {
    _goToFoundLine(_findNthOccurrence(searchString, searchIndex));
  }

  @Override
  public void quickSearchCanceled()
  {
    searchString = null;
    searchIndex = 0;
  }

  private int _findNthOccurrence(String pSearchString, int pN)
  {
    int lastOccurrence = -1;
    int foundOccurrences = 0;

    for (int rowIndex = 0; rowIndex < encodingList.getModel().getSize(); rowIndex++)
    {
      if (_isOccurrence(pSearchString, rowIndex))
      {
        foundOccurrences++;
        lastOccurrence = rowIndex;
        // if n != -1 and n-th result exists (n < foundOccurrences because count found occurrences 1 for one hit, n for first result = 0)
        if (pN >= 0 && foundOccurrences > pN)
        {
          return rowIndex;
        }
      }
    }
    // if n == -1 -> pressed back on first occurrence -> set index to last occurrence
    if (pN < 0)
      searchIndex = foundOccurrences - 1;
    // if result is bigger than biggest occurrence -> Index to 0 (first result) and look up row of first occurrence
    if (pN >= foundOccurrences && foundOccurrences != 0)
    {
      searchIndex = 0;
      return _findNthOccurrence(pSearchString, searchIndex);
    }
    // found nothing -> -1
    if (foundOccurrences == 0)
    {
      return -1;
    }
    // if n < 0 return the row of the last occurrence
    return lastOccurrence;
  }

  /**
   * @param pSearchString string that should be matched to the content of the cell
   * @param pRowIndex     row of the cell to check
   * @return true if the toString method of the object in the cell contains the searchString
   */
  private boolean _isOccurrence(String pSearchString, int pRowIndex)
  {
    Object foundObj = encodingList.getModel().getElementAt(pRowIndex);
    return StringUtils.containsIgnoreCase(foundObj.toString(), pSearchString);
  }

  private void _goToFoundLine(int pFoundRow)
  {
    if (pFoundRow >= 0)
    {
      encodingList.setSelectedIndex(pFoundRow);
      encodingList.scrollRectToVisible(encodingList.getCellBounds(pFoundRow, pFoundRow));
    }
    else
    {
      encodingList.clearSelection();
    }
  }

  @Override
  public boolean isSearchActive()
  {
    return searchString != null;
  }
}

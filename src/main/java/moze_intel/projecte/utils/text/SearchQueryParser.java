package moze_intel.projecte.utils.text;

import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import moze_intel.projecte.PECore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SearchQueryParser {
  private static final Set<Character> TERMINATORS = Set.of('|', '(', '"', '\'');

  public static SearchQueryParser.ISearchQuery parse(String query) {
    if (query != null && !query.isEmpty()) {
      SearchQueryParser.SearchQuery curQuery = new SearchQueryParser.SearchQuery();
      Set<SearchQueryParser.SearchQuery> ret = null;

      for (int i = 0; i < query.length(); i++) {
        char c = query.charAt(i);
        if (c == '|') {
          if (!curQuery.isEmpty()) {
            if (ret == null) {
              ret = new HashSet<>();
            }

            ret.add(curQuery);
          }

          curQuery = new SearchQueryParser.SearchQuery();
        } else if (c != ' ') {
          SearchQueryParser.QueryType type = SearchQueryParser.QueryType.get(c);
          if (type != null) {
            i++;
          } else {
            type = SearchQueryParser.QueryType.NAME;
          }

          SearchQueryParser.KeyListResult keyListResult = readKeyList(query, i, type, curQuery);
          if (!keyListResult.hasResult()) {
            return SearchQueryParser.ISearchQuery.INVALID;
          }

          i = keyListResult.index();
        }
      }

      if (!curQuery.isEmpty()) {
        if (ret == null) {
          return curQuery;
        }

        ret.add(curQuery);
      }

      return ret == null
          ? SearchQueryParser.ISearchQuery.INVALID
          : new SearchQueryParser.SearchQuerySet(ret);
    } else {
      return SearchQueryParser.ISearchQuery.INVALID;
    }
  }

  private static SearchQueryParser.KeyListResult readKeyList(
      String query,
      int start,
      SearchQueryParser.QueryType type,
      SearchQueryParser.SearchQuery curQuery) {
    if (start >= query.length()) {
      return new SearchQueryParser.KeyListResult(true, start);
    }

    char qc = query.charAt(start);
    int newIndex;
    List<String> keys;
    if (qc == '(') {
      SearchQueryParser.ListResult<String> listResult = readList(query, start);
      if (listResult == null) {
        return SearchQueryParser.KeyListResult.INVALID;
      }

      keys = listResult.result();
      newIndex = listResult.index();
    } else if (qc != '"' && qc != '\'') {
      SearchQueryParser.Result textResult =
          readUntilTermination(query, start, type != SearchQueryParser.QueryType.NAME);
      keys = Collections.singletonList(textResult.result());
      newIndex = textResult.index();
    } else {
      SearchQueryParser.Result quoteResult = readQuote(query, start);
      if (quoteResult == null) {
        return SearchQueryParser.KeyListResult.INVALID;
      }

      keys = Collections.singletonList(quoteResult.result());
      newIndex = quoteResult.index();
    }

    if (!keys.isEmpty()) {
      curQuery.queryStrings.put(type, keys);
    }

    return new SearchQueryParser.KeyListResult(true, newIndex);
  }

  @Nullable
  private static SearchQueryParser.ListResult<String> readList(String query, int start) {
    List<String> ret = new ArrayList<>();
    StringBuilder sb = new StringBuilder();

    for (int i = start + 1; i < query.length(); i++) {
      char qc = query.charAt(i);
      switch (qc) {
        case '"':
        case '\'':
          SearchQueryParser.Result quoteResult = readQuote(query, i);
          if (quoteResult == null) {
            return null;
          }

          ret.add(quoteResult.result());
          i = quoteResult.index();
          break;
        case ')':
          String endKey = sb.toString().trim();
          if (!endKey.isEmpty()) {
            ret.add(endKey);
          }

          return new SearchQueryParser.ListResult<>(ret, i);
        case '|':
          String parsedKey = sb.toString().trim();
          if (!parsedKey.isEmpty()) {
            ret.add(parsedKey);
          }

          sb = new StringBuilder();
          break;
        default:
          sb.append(qc);
      }
    }

    return null;
  }

  @Nullable
  private static SearchQueryParser.Result readQuote(String text, int start) {
    char quoteChar = text.charAt(start);
    StringBuilder ret = new StringBuilder();

    for (int i = start + 1; i < text.length(); i++) {
      char tc = text.charAt(i);
      if (tc == quoteChar) {
        return new SearchQueryParser.Result(ret.toString(), i);
      }

      ret.append(tc);
    }

    return null;
  }

  private static SearchQueryParser.Result readUntilTermination(
      String text, int start, boolean spaceTerminate) {
    StringBuilder sb = new StringBuilder();

    int i;
    for (i = start; i < text.length(); i++) {
      char tc = text.charAt(i);
      if (TERMINATORS.contains(tc)
          || SearchQueryParser.QueryType.get(tc) != null
          || spaceTerminate && tc == ' ') {
        i--;
        break;
      }

      sb.append(tc);
    }

    return new SearchQueryParser.Result(sb.toString().trim(), i);
  }

  @FunctionalInterface
  public interface ISearchQuery {
    SearchQueryParser.ISearchQuery INVALID = (level, player, stack) -> false;

    boolean test(@Nullable Level var1, @Nullable Player var2, ItemStack var3);

    default boolean isInvalid() {
      return this == INVALID;
    }
  }

  private record KeyListResult(boolean hasResult, int index) {
    public static final SearchQueryParser.KeyListResult INVALID =
        new SearchQueryParser.KeyListResult(false, -1);
  }

  private record ListResult<TYPE>(List<TYPE> result, int index) {}

  public enum QueryType {
    NAME('~') {
      @Override
      public boolean matches(
          @Nullable Level level, @Nullable Player player, String key, ItemStack stack) {
        String search = key.toLowerCase(Locale.ROOT);
        String itemId =
            stack
                .typeHolder()
                .unwrapKey()
                .map(keyResource -> keyResource.identifier().toString().toLowerCase(Locale.ROOT))
                .orElse("");
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)
            || itemId.contains(search)
            || ItemNameSearchIndex.contains(stack.getItem().getDescriptionId(), search);
      }
    },
    MOD_ID('@') {
      @Override
      public boolean matches(
          @Nullable Level level, @Nullable Player player, String key, ItemStack stack) {
        Item item = stack.getItem();
        String modid =
            stack
                .typeHolder()
                .unwrapKey()
                .map(keyResource -> keyResource.identifier().getNamespace())
                .orElse(null);
        if (modid == null) {
          PECore.LOGGER.error(
              "Unexpected null registry name for item of class type: {}",
              item.getClass().getSimpleName());
          return false;
        } else {
          return modid.toLowerCase(Locale.ROOT).contains(key);
        }
      }
    },
    TOOLTIP('$') {
      @Override
      public boolean matches(
          @Nullable Level level, @Nullable Player player, String key, ItemStack stack) {
        for (Component tooltipLine :
            stack.getTooltipLines(TooltipContext.of(level), player, Default.NORMAL)) {
          String tooltip = tooltipLine.getString().toLowerCase(Locale.ROOT);
          if (tooltip.contains(key)) {
            return true;
          }
        }

        return false;
      }
    },
    TAG('#') {
      @Override
      public boolean matches(
          @Nullable Level level, @Nullable Player player, String key, ItemStack stack) {
        return stack
            .typeHolder()
            .tags()
            .anyMatch(tag -> tag.location().toString().toLowerCase(Locale.ROOT).contains(key));
      }
    };

    private static final Char2ObjectMap<SearchQueryParser.QueryType> charLookupMap;
    private final char prefix;

    public static SearchQueryParser.QueryType get(char prefix) {
      return (SearchQueryParser.QueryType) charLookupMap.get(prefix);
    }

    public static CharSet getPrefixChars() {
      return charLookupMap.keySet();
    }

    QueryType(char prefix) {
      this.prefix = prefix;
    }

    public abstract boolean matches(
        @Nullable Level var1, @Nullable Player var2, String var3, ItemStack var4);

    static {
      SearchQueryParser.QueryType[] values = values();
      charLookupMap = new Char2ObjectArrayMap(values.length);

      for (SearchQueryParser.QueryType type : values) {
        charLookupMap.put(type.prefix, type);
      }
    }
  }

  private record Result(String result, int index) {}

  public static class SearchQuery implements SearchQueryParser.ISearchQuery {
    private final Map<SearchQueryParser.QueryType, List<String>> queryStrings =
        new EnumMap<>(SearchQueryParser.QueryType.class);

    @Override
    public boolean test(@Nullable Level level, @Nullable Player player, ItemStack stack) {
      for (Entry<SearchQueryParser.QueryType, List<String>> entry : this.queryStrings.entrySet()) {
        boolean hasNoMatch = true;

        for (String key : entry.getValue()) {
          if (entry.getKey().matches(level, player, key, stack)) {
            hasNoMatch = false;
            break;
          }
        }

        if (hasNoMatch) {
          return false;
        }
      }

      return true;
    }

    private boolean isEmpty() {
      return this.queryStrings.isEmpty();
    }

    protected Map<SearchQueryParser.QueryType, List<String>> getQueryMap() {
      return this.queryStrings;
    }

    @Override
    public String toString() {
      return this.queryStrings.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else {
        return o != null && this.getClass() == o.getClass()
            ? this.queryStrings.equals(((SearchQueryParser.SearchQuery) o).queryStrings)
            : false;
      }
    }

    @Override
    public int hashCode() {
      return this.queryStrings.hashCode();
    }
  }

  public static class SearchQuerySet implements SearchQueryParser.ISearchQuery {
    private final Set<SearchQueryParser.SearchQuery> queries;

    private SearchQuerySet(Set<SearchQueryParser.SearchQuery> queries) {
      this.queries = queries;
    }

    @Override
    public boolean test(@Nullable Level level, @Nullable Player player, ItemStack stack) {
      if (this.queries.isEmpty()) {
        return true;
      }

      for (SearchQueryParser.SearchQuery query : this.queries) {
        if (query.test(level, player, stack)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public String toString() {
      return this.queries.toString();
    }

    protected Set<SearchQueryParser.SearchQuery> getQueries() {
      return this.queries;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else {
        return o != null && this.getClass() == o.getClass()
            ? this.queries.equals(((SearchQueryParser.SearchQuerySet) o).queries)
            : false;
      }
    }

    @Override
    public int hashCode() {
      return this.queries.hashCode();
    }
  }
}

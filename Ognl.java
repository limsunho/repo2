import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * OGNL (Object Graph Navigation Library)
 * 
 * MyBatis에서 isEmpty, isNotEmpty 구문이 사라져서 아래 함수를 이용해서 비슷한 구현을 하도록 한다.
 * Null 비교를 할 수 있는 Object는 다음과 같다.
 * 
 * 1) String
 * 2) Collection
 * 3) Map
 * 4) Array
 * 
 * MyBatis의 Mapper.xml에서는 다음과 같이 사용하도록 한다.
 * 
 * <if test="@Ognl@isEmpty(value)">...</if>
 * 
 * <if test="@Ognl@isNotEmpty(value)">...</if>
 * 
 * @author jhkang
 * @since 2012-03-16
 */
public class Ognl {
	
	/**
	 * Test for Map, Collection, String, Array isEmpty
	 * 
	 * @param obj
	 * @return boolean
	 * @throws IllegalArgumentException
	 */
	public static boolean isEmpty(Object obj) throws IllegalArgumentException {
		if (obj == null) {
			return true;
		}

		if (obj instanceof String) {
			return "".equals(obj.toString().trim());
		} else if (obj instanceof Collection) {
			return ((Collection) obj).isEmpty();
		} else if (obj instanceof Map) {
			return ((Map) obj).isEmpty();
		} else if (obj instanceof Object[]) {
			return Array.getLength(obj) == 0;
		} else {
			return obj == null;
		}
	}
	
	/**
	 * Test for Map, Collection, String, Array isNotEmpty
	 * 
	 * @param obj
	 * @return boolean
	 * @throws IllegalArgumentException
	 */
	public static boolean isNotEmpty(Object obj) throws IllegalArgumentException {
		return !isEmpty(obj);
	}
	
	/**
	 * 객체를 받아서 toString후 substring해서 반환한다.
	 * @author Woong-Chul, Choi 
	 * @param obj
	 * @param beginIndex
	 * @param endIndex
	 * @return
	 */
	public static String substring(Object obj, int beginIndex, int endIndex) {
		String str = obj.toString();
		
		str = str.substring(beginIndex, endIndex);
		
		return str;		
	}

	/**
	 * 객체를 받아서 toString후  length를 반환한다.
	 * @author shlim 
	 * @param obj
	 * @return
	 */
	public static int length(Object obj) {
		String str = obj.toString();

		return str.length();
	}
	
}
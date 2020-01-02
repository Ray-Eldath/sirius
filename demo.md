### Still in progress

**May changed at any time.**

```kotlin
jsonObject {
    length in 3..6
    ignore("comment", "comments")

    any {
        "a_string" string {
            required
            length in 5..10
            // or: minLength=12 or maxLength=12
            it oneof("a", "b")
            test { }
        }
    
        "a_number" integer {
            required
            it in 14.0..123.2
            // or: min = 14.0 or max = 123.2
            length in 2..3
        }
    }

    group {
        "b" string()
        "a" number()
    }
    
    "a_array" jsonArray {
        every {
        }

        any {
            string {
                it oneof("b", "c", "d")
                it regex("XXX")
            }

            number {
                type == NumberType.Integer
            }
        }
    }
}
```
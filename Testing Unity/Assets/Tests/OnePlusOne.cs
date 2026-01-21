using System.Collections;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;

public class OnePlusOne
{
    // A Test behaves as an ordinary method
    [Test]
    public void OnePlusOneSimplePasses()
    {
        // Use the Assert class to test conditions
        Assert.AreEqual(1 + 1, 2);
    }

    // A UnityTest behaves like a coroutine in Play Mode. In Edit Mode you can use
    // `yield return null;` to skip a frame.
    [UnityTest]
    public IEnumerator OnePlusOneWithEnumeratorPasses()
    {
        // Use the Assert class to test conditions.
        // Use yield to skip a frame.
        Assert.AreEqual(1 + 1, 2);
        yield return null;
    }
}
